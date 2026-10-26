package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R13 continuous creation and evolution of complete model strategies
 * (P20 families, P21 operators, P22 feedback-directed repair, P23 retained
 * parents / Hall of Fame, P24 effective generations, P25 frequency tiers,
 * P26 fitness). Mirrors foundry13.campaign() in the Python reference.
 *
 * A campaign continues the previous State (population, Hall of Fame, director
 * statistics, tier champions): evolution never restarts, good parents are never
 * thrown away, and every child is replayed on history before it is ranked.
 * The campaign reads nothing at or after E.valEnd; SEALED/live blocks are
 * scored by the caller.
 */
public final class ModelEvolutionSU2 {
    public static final String[] OPS = {"mutate", "add", "remove", "reweight", "pool", "gate", "coverage", "crossover", "fusion", "diagnose", "prune"};
    /** R14b: + "credit" (atom-credit-guided child of an ATOMNET member, or graft the atom network onto a program). */
    public static final String[] OPS_R14B = {"mutate", "add", "remove", "reweight", "pool", "gate", "coverage", "crossover", "fusion", "diagnose", "prune", "credit"};
    /** R15: + "distill" (atomic distillation: explain any parent in atoms and rebuild it as an atom network). */
    public static final String[] OPS_R15 = {"mutate", "add", "remove", "reweight", "pool", "gate", "coverage", "crossover", "fusion", "diagnose", "prune", "credit", "distill"};
    public static final String[] TIERS = {"F15", "F30", "F100"};

    /** Parameter grids, written exactly as the Python reference formats them (string identity = cache identity). */
    public static final Map<String, LinkedHashMap<String, String[]>> GRID = new LinkedHashMap<>();
    static {
        GRID.put("MSDIR", grid("scales", new String[]{"(0.9, 0.97, 0.99, 0.997, 1.0)", "(0.95, 0.99, 0.998)", "(0.8, 0.9, 0.95, 0.98)"}, "alpha", new String[]{"0.5", "1", "3"}, "share", new String[]{"0.003", "0.01", "0.05"}));
        GRID.put("BOCPD", grid("hazard", new String[]{"0.02", "0.01", "0.005", "0.0025"}, "alpha", new String[]{"0.3", "1", "3"}, "R", new String[]{"160"}));
        GRID.put("HMM", grid("K", new String[]{"2", "3", "4"}, "stay", new String[]{"0.98", "0.995", "0.999"}, "lr", new String[]{"0.003", "0.01", "0.03"}, "seed", new String[]{"0", "1"}, "temper", new String[]{"0.3", "0.6"}));
        GRID.put("LOGLIN", grid("sources", new String[]{"((0, 1),)", "((0, 1), (1, 1), (5, 1))", "((3, 1), (7, 1), (0, 2))", "((1, 1), (2, 1), (4, 1), (6, 1), (8, 1))", "((0, 1), (0, 2), (9, 1), (2, 2))"}, "lr", new String[]{"0.02", "0.05", "0.1"}, "l2", new String[]{"0.0001", "0.001"}));
        GRID.put("KALMAN", grid("q_level", new String[]{"5e-06", "2e-05", "0.0001"}, "q_slope", new String[]{"0", "2e-08", "1e-07"}, "horizon", new String[]{"1", "20"}, "temp", new String[]{"8", "16"}));
        GRID.put("RELBAYES", grid("lags", new String[]{"(1,)", "(1, 2)", "(1, 2, 3)"}, "decay", new String[]{"0.99", "0.995", "0.998"}, "kappa", new String[]{"30", "100", "300"}));
        // R16 (P48) regime-memory families (regmem.GRID / regmem.RELGRID); only generated when r16
        GRID.put("REGMEM", grid("K", new String[]{"2", "3", "4"}, "stay", new String[]{"0.98", "0.995", "0.999"}, "lr", new String[]{"0.003", "0.01", "0.03"}, "seed", new String[]{"0", "1"}, "temper", new String[]{"0.3", "0.6"}));
        GRID.put("RELMEM", grid("K", new String[]{"1", "2"}, "lags", new String[]{"()", "(1,)", "(1, 2)"}, "sources", new String[]{"((0, 1),)", "()", "((3, 2), (7, 2))"}, "stay", new String[]{"0.98", "0.995", "0.999"},
                                "lr", new String[]{"0.01", "0.02", "0.05"}, "l2", new String[]{"0.001", "0.0001"}, "temper", new String[]{"0.3", "0.6"}, "seed", new String[]{"0", "1"}));
    }
    static LinkedHashMap<String, String[]> grid(Object... kv) { LinkedHashMap<String, String[]> m = new LinkedHashMap<>(); for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (String[]) kv[i + 1]); return m; }

    public static final class Eras { public final int devStart, devEnd, valStart, valEnd; public Eras(int a, int b, int c, int d) { if (!(a < b && b <= c && c < d)) throw new IllegalArgumentException("SU2_ERAS"); devStart = a; devEnd = b; valStart = c; valEnd = d; } }

    public static final class Ranked { public final ModelGenomeSU2 g; public final String origin, parent, note; Ranked(ModelGenomeSU2 g, String origin, String parent, String note) { this.g = g; this.origin = origin; this.parent = parent; this.note = note; } }

    public static final class State {
        public final List<Ranked> population = new ArrayList<>(), hallOfFame = new ArrayList<>();
        public final Map<String, Ranked> tiers = new TreeMap<>();
        public Ranked champion;
        public final Map<String, double[]> kindStats = new TreeMap<>();   // kind -> {n, gain}
        public final Map<String, double[]> opStats = new TreeMap<>();     // op -> {n, win}
        public final List<String> repairNotes = new ArrayList<>();
        public int effectiveGenerations;                                   // P24: counted only on a VAL improvement
        /** R14 (P37): per-tier committee champions (geometric-mean fusion of the top programs by the judge). */
        public final Map<String, CommitteeSU2> committees = new TreeMap<>();
        public double committeeJudge = -9;                                 // F15 committee's judge value (effective generations)
        /** R15 (P44): research memory - atom index -> {programs that used it, running mean of normalised credit}. */
        public final Map<Integer, double[]> atomPrior = new TreeMap<>();
        public final Set<String> priorSeen = new LinkedHashSet<>();
    }

    final ModelReplaySU2.Library lib; final Random rng; final int startMeta, window, refit;
    final Map<String, Double> devFit = new HashMap<>(), valFit = new HashMap<>();
    final Map<String, ModelReplaySU2.Result> results = new LinkedHashMap<String, ModelReplaySU2.Result>(64, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, ModelReplaySU2.Result> e) { return size() > 400; }
    };
    /** R14 engine (P36-P38): calibrated programs, consensus judge, committee champions. false = the R13 engine. */
    public final boolean r14;
    /** R14b engine (P39-P41): ATOMNET members with atom-credit-guided children, open LOGLIN relation search,
     *  empirical-Bayes judge, family-capped consensus. Requires r14. */
    public final boolean r14b;
    /** R15 engine (P43-P46): full atom world (GAP / decomposed XI atoms, 112 state atoms, learned molecules), teacher
     *  matrix pool, research memory, decomposed-XI seeds. Requires r14b. */
    public final boolean r15;
    /** R16 engine (P48): regime-memory families REGMEM / RELMEM (anchored null state; patterns and relation sets kept while
     *  their regime is absent) in the creation space, the consensus, the seeds and the feedback diagnosis. Requires r15. */
    public final boolean r16;
    public ModelEvolutionSU2(ModelReplaySU2.Library lib, long seed, int startMeta, int window, int refit) { this(lib, seed, startMeta, window, refit, true, true, true, true); }
    public ModelEvolutionSU2(ModelReplaySU2.Library lib, long seed, int startMeta, int window, int refit, boolean r14) { this(lib, seed, startMeta, window, refit, r14, false, false); }
    public ModelEvolutionSU2(ModelReplaySU2.Library lib, long seed, int startMeta, int window, int refit, boolean r14, boolean r14b) { this(lib, seed, startMeta, window, refit, r14, r14b, false); }
    public ModelEvolutionSU2(ModelReplaySU2.Library lib, long seed, int startMeta, int window, int refit, boolean r14, boolean r14b, boolean r15) { this(lib, seed, startMeta, window, refit, r14, r14b, r15, false); }
    public ModelEvolutionSU2(ModelReplaySU2.Library lib, long seed, int startMeta, int window, int refit, boolean r14, boolean r14b, boolean r15, boolean r16) {
        this.lib = lib; rng = new Random(seed); this.startMeta = startMeta; this.window = window; this.refit = refit;
        this.r14 = r14 || r14b || r15 || r16; this.r14b = r14b || r15 || r16; this.r15 = r15 || r16; this.r16 = r16;
    }

    public static String tierOf(ModelGenomeSU2 g) { return g.coverage >= .999 ? "F100" : g.coverage >= .30 ? "F30" : "F15"; }

    ModelReplaySU2.Result eval(ModelGenomeSU2 g) { return results.computeIfAbsent(g.key(), k -> ModelReplaySU2.evaluate(g, lib, startMeta, window, refit)); }
    double dev(ModelGenomeSU2 g, Eras e) { return devFit.computeIfAbsent(g.key(), k -> r14 ? judge(eval(g), e.devStart, e.devEnd, 3) : ModelReplaySU2.fitness(eval(g), e.devStart, e.devEnd, 3, 0.12, 200)); }
    /** R13 v2: champion/tier choice on all draws before the checkpoint (DEV+VAL, 4 folds); VAL alone carried no forward signal. */
    double val(ModelGenomeSU2 g, Eras e) { return valFit.computeIfAbsent(g.key(), k -> r14 ? judge(eval(g), e.devStart, e.valEnd, 4) : ModelReplaySU2.fitness(eval(g), e.devStart, e.valEnd, 4, 0.12, 200)); }

    // ---------------------------------------------------------------- R14 consensus judge (P38)
    /** Consensus Q[t][p][n] for the current generation (uniform before CalibrationSU2.START). */
    float[][][] Q;
    final Map<String, float[][][]> tables = new LinkedHashMap<String, float[][][]>(32, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, float[][][]> e) { return size() > 40; } };
    final Map<String, double[]> cumLL = new LinkedHashMap<String, double[]>(32, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, double[]> e) { return size() > 200; } };

    float[][][] table(ModelGenomeSU2 g) { return tables.computeIfAbsent(tableKey(g), k -> ModelReplaySU2.pooledTable(g, lib)); }

    /** Identity of a program's probability table: gate and frequency do not change the table. */
    static String tableKey(ModelGenomeSU2 g) {
        StringBuilder b = new StringBuilder(g.calibrated ? "CAL|" : "RAW|").append(g.members.size() > 1 ? g.pool : "STATIC").append(g.sink ? "|S|" : "|N|");
        for (ModelGenomeSU2.Member m : g.members) b.append(m.text()).append(';');
        return b.toString();
    }

    /** cumulative per-period log-likelihood of the realised draw over all 10 positions */
    double[] cum(ModelGenomeSU2 g) {
        return cumLL.computeIfAbsent(tableKey(g), k -> {
            float[][][] P = table(g); int T = P.length; double[] c = new double[T + 1]; double[] row = new double[10];
            for (int t = 0; t < T; t++) { for (int p = 0; p < 10; p++) row[p] = Math.log(Math.min(1.0, Math.max(1e-9, (double) P[t][p][lib.draws()[t][p] - 1]))); c[t + 1] = c[t] + ModelOpsSU2.pairwise(row, 0, 10); }
            return c;
        });
    }

    /** Bayesian model average of the programs' tables, weights from the trailing-window log-likelihood. */
    void buildConsensus(List<ModelGenomeSU2> progs) {
        int T = lib.draws().length, M = progs.size(); float[][][][] tabs = new float[M][][][]; double[][] cs = new double[M][];
        for (int m = 0; m < M; m++) { tabs[m] = table(progs.get(m)); cs[m] = cum(progs.get(m)); }
        float[][][] q = new float[T][10][10];
        for (int t = 0; t < Math.min(CalibrationSU2.START, T); t++) for (int p = 0; p < 10; p++) Arrays.fill(q[t][p], 0.1f);
        for (int t0 = CalibrationSU2.START; t0 < T; t0 += CalibrationSU2.REFIT) {
            int lo = Math.max(0, t0 - CalibrationSU2.WINDOW); double[] s = new double[M]; double mx = Double.NEGATIVE_INFINITY;
            for (int m = 0; m < M; m++) { s[m] = cs[m][t0] - cs[m][lo]; mx = Math.max(mx, s[m]); }
            double[] w = new double[M]; for (int m = 0; m < M; m++) w[m] = Math.exp(s[m] - mx); double z = ModelOpsSU2.pairwise(w, 0, M); for (int m = 0; m < M; m++) w[m] /= z;
            for (int t = t0; t < Math.min(t0 + CalibrationSU2.REFIT, T); t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                double v = 0; for (int m = 0; m < M; m++) v += w[m] * tabs[m][t][p][n]; q[t][p][n] = (float) v; }
        }
        Q = q;
    }

    static final double EB_KAPPA = 300;

    /** R14b judge (P40): realised hits shrunk toward the consensus expectation of the same decisions,
     *  J = (hits + kappa * mean_acted Q-expected) / (acts + kappa) - coverage penalty (foundry14.judge_eb). */
    double judgeEb(ModelReplaySU2.Result r, int a, int b) {
        int n = 0, k = 0; double s = 0;
        for (int t = a; t < b; t++) if (r.act[t]) { n++; if (r.hit[t]) k++; int p = r.position[t] - 1; double e = 0; for (int j = 0; j < 10; j++) if ((r.mask[t] & (1 << j)) != 0) e += Q[t][p][j]; s += e; }
        double q = n > 0 ? s / n : 0.3, cov = n / (double) (b - a), pen = cov >= 0.12 ? 0 : (0.12 - cov) * 2;
        return (k + EB_KAPPA * q) / (n + EB_KAPPA) - pen;
    }

    /** Judge fitness: like ModelReplaySU2.fitness, but an acted period counts its expected hit under the consensus. */
    double judge(ModelReplaySU2.Result r, int a, int b, int folds) {
        if (r14b) return judgeEb(r, a, b);
        double[] v = new double[folds]; int nAll = 0; double kappa = 200;
        for (int f = 0; f < folds; f++) {
            int x = a + (int) Math.floor((b - a) * (double) f / folds), y = a + (int) Math.floor((b - a) * (double) (f + 1) / folds);
            int n = 0; double s = 0;
            for (int t = x; t < y; t++) if (r.act[t]) { n++; int p = r.position[t] - 1; for (int k = 0; k < 10; k++) if ((r.mask[t] & (1 << k)) != 0) s += Q[t][p][k]; }
            nAll += n; v[f] = (s + 0.3 * kappa) / (n + kappa);
        }
        double mean = 0, mx = -1, mn = 2; for (double q : v) { mean += q; mx = Math.max(mx, q); mn = Math.min(mn, q); }
        mean /= folds; double cov = nAll / (double) (b - a), pen = cov >= 0.12 ? 0 : (0.12 - cov) * 2;
        return mean - 0.25 * (mx - mn) - pen;
    }

    static List<ModelGenomeSU2> centralPrograms() { return centralPrograms(false); }
    static List<ModelGenomeSU2> centralPrograms(boolean withAtomNet) { return centralPrograms(withAtomNet, false); }
    /** Python foundry14.central_programs order: the model kinds (R16 kinds after the six), XI_BASE is Python-only, ATOMNET. */
    static List<ModelGenomeSU2> centralPrograms(boolean withAtomNet, boolean withRegimeMemory) {
        List<ModelGenomeSU2> out = new ArrayList<>();
        for (String kind : ModelGenomeSU2.KINDS) out.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind, centralParams(kind), 1.0)), "STATIC", false, "MASS", 0.30, 0.2, true));
        if (withRegimeMemory) for (String kind : ModelGenomeSU2.R16_KINDS) out.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind, centralParams(kind), 1.0)), "STATIC", false, "MASS", 0.30, 0.2, true));
        if (withAtomNet) out.add(new ModelGenomeSU2(Collections.singletonList(defaultAtomNet()), "STATIC", false, "MASS", 0.30, 0.2, true));
        return out;
    }

    // ---------------------------------------------------------------- R14b: ATOMNET members (P39), open relations (P41)
    static int atomIndex(String spec) { for (int i = 0; i < AtomStreamSU2.SPECS.length; i++) if (AtomStreamSU2.SPECS[i].equals(spec)) return i; throw new IllegalArgumentException("SU2_ATOM_SPEC " + spec); }
    /** The multi-scale spanning set (atomnet.default_atoms). */
    static int[] defaultAtoms() {
        String[] specs = {"FREQUENCY|window=10", "FREQUENCY|window=30", "FREQUENCY|window=120", "FREQUENCY|window=500", "HOT_COLD_Z|recent=20,reference=120",
            "OMISSION|", "OMISSION_RATE|window=50", "FREQUENCY_SLOPE|window=50", "MARKOV_SCORE|alpha=0.5,order=1,window=300", "LAG_DIGIT_EQ|lag=1",
            "LAG_DIGIT_EQ|lag=2", "GAP_LAST_GAP|window=120"};
        int[] out = new int[specs.length]; for (int i = 0; i < specs.length; i++) out[i] = atomIndex(specs[i]); Arrays.sort(out); return out;
    }
    static String joinInts(int[] a) { if (a.length == 0) return "none"; StringBuilder b = new StringBuilder(); for (int i = 0; i < a.length; i++) { if (i > 0) b.append('-'); b.append(a[i]); } return b.toString(); }
    static String joinPairs(List<int[]> ps) {
        if (ps.isEmpty()) return "none";
        TreeSet<String> s = new TreeSet<>((x, y) -> { String[] a = x.split("x"), b = y.split("x"); int c = Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])); return c != 0 ? c : Integer.compare(Integer.parseInt(a[1]), Integer.parseInt(b[1])); });
        for (int[] q : ps) s.add(Math.min(q[0], q[1]) + "x" + Math.max(q[0], q[1]));
        return String.join("-", s);
    }
    static String g4(double v) { return ModelGenomeSU2.fmt(v); }
    static ModelGenomeSU2.Member atomNet(Collection<Integer> atoms, List<int[]> pairs, Collection<Integer> lags, boolean group, double lr, double l2, double gd) {
        int[] a = new TreeSet<>(atoms).stream().mapToInt(Integer::intValue).toArray(); int[] l = new TreeSet<>(lags).stream().mapToInt(Integer::intValue).toArray();
        Map<String, String> p = new TreeMap<>();
        p.put("atoms", joinInts(a)); p.put("pairs", joinPairs(pairs)); p.put("lags", joinInts(l)); p.put("group", group ? "1" : "0");
        p.put("lr", g4(lr)); p.put("l2", g4(l2)); p.put("gd", g4(gd));
        return new ModelGenomeSU2.Member("ATOMNET", p, 1.0);
    }
    static ModelGenomeSU2.Member defaultAtomNet() {
        List<Integer> a = new ArrayList<>(); for (int x : defaultAtoms()) a.add(x);
        return atomNet(a, new ArrayList<>(), new ArrayList<>(), false, 0.05, 1e-4, 0.999);
    }
    int universe() { return r15 ? AtomStreamSU2.size() : AtomStreamSU2.r14bSize(); }
    State curState;
    /** Research memory (R15): an atom drawn with probability proportional to its accumulated credit (+ exploration). */
    int priorPick(Collection<Integer> exclude) {
        int U = universe();
        if (!r15 || curState == null || curState.atomPrior.isEmpty()) { int a; do { a = rng.nextInt(U); } while (exclude.contains(a) && exclude.size() < U); return a; }
        double[] w = new double[U]; double z = 0;
        for (int i = 0; i < U; i++) { if (exclude.contains(i)) continue; double[] v = curState.atomPrior.get(i); w[i] = (v == null ? 0 : v[1]) + 0.02; z += w[i]; }
        double u = rng.nextDouble() * z; for (int i = 0; i < U; i++) { if (w[i] == 0) continue; u -= w[i]; if (u <= 0) return i; }
        return U - 1;
    }
    static List<Integer> xiAtoms() { List<Integer> out = new ArrayList<>(); for (int i = 0; i < AtomStreamSU2.size(); i++) if (AtomStreamSU2.CATALOG[i].family.startsWith("XI_")) out.add(i); return out; }
    static String joinMols(List<int[]> ms) {
        if (ms.isEmpty()) return "none";
        TreeSet<String> s = new TreeSet<>((x, y) -> { String[] a = x.split("@"), b = y.split("@"); int c = Integer.compare(Integer.parseInt(a[0]), Integer.parseInt(b[0])); return c != 0 ? c : Integer.compare(Integer.parseInt(a[1]), Integer.parseInt(b[1])); });
        for (int[] m : ms) s.add(m[0] + "@" + m[1]);
        return String.join("-", s);
    }
    static ModelGenomeSU2.Member atomNet(Collection<Integer> atoms, List<int[]> pairs, Collection<Integer> lags, boolean group, double lr, double l2, double gd, List<int[]> mols) {
        ModelGenomeSU2.Member m = atomNet(atoms, pairs, lags, group, lr, l2, gd);
        List<int[]> keep = new ArrayList<>(); for (int[] x : mols) if (atoms.contains(x[0])) keep.add(x);
        if (keep.isEmpty()) return m;
        Map<String, String> p = new TreeMap<>(m.params); p.put("mols", joinMols(keep));
        return new ModelGenomeSU2.Member("ATOMNET", p, 1.0);
    }
    ModelGenomeSU2.Member randomAtomNet() {
        int[] base = defaultAtoms(); int n = 3 + rng.nextInt(8); TreeSet<Integer> atoms = new TreeSet<>();
        List<Integer> b = new ArrayList<>(); for (int x : base) b.add(x); Collections.shuffle(b, rng);
        for (int i = 0; i < Math.max(1, n / 2) && i < b.size(); i++) atoms.add(b.get(i));
        while (atoms.size() < n) atoms.add(r15 ? priorPick(atoms) : rng.nextInt(universe()));
        List<int[]> pairs = new ArrayList<>();
        if (rng.nextDouble() < 0.25 && atoms.size() >= 2) { List<Integer> al = new ArrayList<>(atoms); Collections.shuffle(al, rng); pairs.add(new int[]{al.get(0), al.get(1)}); }
        int[][] lagChoices = {{}, {}, {1}, {1, 2}}; List<Integer> lags = new ArrayList<>(); for (int x : lagChoices[rng.nextInt(4)]) lags.add(x);
        List<int[]> mols = new ArrayList<>();
        if (r15 && rng.nextDouble() < 0.3) { List<Integer> al = new ArrayList<>(atoms); int k = 1 + rng.nextInt(2); for (int i = 0; i < k; i++) mols.add(new int[]{al.get(rng.nextInt(al.size())), rng.nextInt(AtomStreamSU2.states())}); }
        return atomNet(atoms, pairs, lags, rng.nextDouble() < 0.3, new double[]{0.02, 0.05, 0.1}[rng.nextInt(3)], new double[]{1e-4, 1e-3}[rng.nextInt(2)], new double[]{1.0, 0.999, 0.995}[rng.nextInt(3)], mols);
    }

    /** Learned credit of an ATOMNET member at the end of the training window (draws < valEnd only). */
    final Map<String, Map<String, Double>> creditCache = new LinkedHashMap<String, Map<String, Double>>(32, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Double>> e) { return size() > 64; } };
    Eras curEras;
    Map<String, Double> credit(ModelGenomeSU2.Member m) {
        if (curEras == null) return null;
        int end = Math.min(curEras.valEnd, lib.draws().length);
        return creditCache.computeIfAbsent(m.modelKey() + "@" + end, k -> { AtomNetSU2 net = AtomNetSU2.of(m.params); for (int t = 0; t < end; t++) net.update(lib.draws()[t]); return net.credit(); });
    }
    /** Research memory: every atom network credited at this checkpoint reports what it learned (once per program). */
    void updatePrior(State s) {
        for (Map.Entry<String, Map<String, Double>> e : new ArrayList<>(creditCache.entrySet())) {
            if (!s.priorSeen.add(e.getKey())) continue;
            if (s.priorSeen.size() > 20000) { Iterator<String> it = s.priorSeen.iterator(); it.next(); it.remove(); }
            double tot = 0; int n = 0; for (Map.Entry<String, Double> c : e.getValue().entrySet()) if (c.getKey().startsWith("A")) { tot += c.getValue(); n++; }
            if (n == 0 || tot <= 0) continue;
            for (Map.Entry<String, Double> c : e.getValue().entrySet()) {
                if (!c.getKey().startsWith("A")) continue;
                int i = Integer.parseInt(c.getKey().substring(1)); double[] v = s.atomPrior.computeIfAbsent(i, k -> new double[2]);
                v[0] += 1; v[1] += (c.getValue() / tot * n - v[1]) / v[0];
            }
        }
    }

    // ---------------------------------------------------------------- R15 atomic distillation (P47)
    public static final int DISTILL_STRIDE = 3, DISTILL_K = 8; public static final double DISTILL_RIDGE = 1e-3;
    public static final class Distilled { public final int[] atoms; public final double[] weights; public final double r2; Distilled(int[] a, double[] w, double r2) { atoms = a; weights = w; this.r2 = r2; } }
    final Map<String, Distilled> distillCache = new HashMap<>();

    List<Integer> distillCandidates(State s) {
        TreeSet<Integer> c = new TreeSet<>(); for (int x : defaultAtoms()) c.add(x);
        for (int i = AtomStreamSU2.r14bSize(); i < AtomStreamSU2.size(); i++) c.add(i);
        if (s != null && !s.atomPrior.isEmpty()) {
            List<Map.Entry<Integer, double[]>> l = new ArrayList<>(s.atomPrior.entrySet()); l.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));
            for (int i = 0; i < Math.min(12, l.size()); i++) c.add(l.get(i).getKey());
        }
        for (int i = 0; i < 10; i++) c.add(rng.nextInt(AtomStreamSU2.size()));
        return new ArrayList<>(c);
    }

    /** Ridge fit of the parent's centred log-table on centred atom inputs over [1500, end) (every 3rd period). */
    public static Distilled distill(float[][][] P, int[][] draws, List<Integer> cand, int end, int k) {
        int F = cand.size(); double[][] XtX = new double[F][F]; double[] Xty = new double[F], sx = new double[F], sxx = new double[F]; double ss = 0; long n = 0, nx = 0;
        AtomStreamSU2 st = new AtomStreamSU2(); double[][] x = new double[F][10];
        for (int t = 0; t < end; t++) {
            if (t >= 1500 && (t - 1500) % DISTILL_STRIDE == 0) {
                float[][][] A = new float[F][][]; for (int f = 0; f < F; f++) A[f] = st.atom(cand.get(f));
                for (int p = 0; p < 10; p++) {
                    double[] y = new double[10]; double my = 0; for (int q = 0; q < 10; q++) { y[q] = Math.log(Math.min(1.0, Math.max(1e-9, (double) P[t][p][q]))); my += y[q]; } my /= 10;
                    for (int f = 0; f < F; f++) { double m = 0; for (int q = 0; q < 10; q++) { m += A[f][p][q]; sx[f] += A[f][p][q]; sxx[f] += (double) A[f][p][q] * A[f][p][q]; } m /= 10; for (int q = 0; q < 10; q++) x[f][q] = A[f][p][q] - m; }
                    nx += 10;
                    for (int q = 0; q < 10; q++) {
                        double yc = y[q] - my; ss += yc * yc; n++;
                        for (int f = 0; f < F; f++) { Xty[f] += x[f][q] * yc; for (int g = f; g < F; g++) XtX[f][g] += x[f][q] * x[g][q]; }
                    }
                }
            }
            st.update(draws[t]);
        }
        if (n == 0) return null;
        double[][] Aug = new double[F][F + 1];
        for (int f = 0; f < F; f++) { for (int g = 0; g < F; g++) Aug[f][g] = (g >= f ? XtX[f][g] : XtX[g][f]) / n + (f == g ? DISTILL_RIDGE : 0); Aug[f][F] = Xty[f] / n; }
        for (int c = 0; c < F; c++) {                                           // Gaussian elimination, partial pivoting
            int piv = c; for (int r = c + 1; r < F; r++) if (Math.abs(Aug[r][c]) > Math.abs(Aug[piv][c])) piv = r;
            double[] tmp = Aug[c]; Aug[c] = Aug[piv]; Aug[piv] = tmp;
            for (int r = 0; r < F; r++) if (r != c) { double fct = Aug[r][c] / Aug[c][c]; for (int q = c; q <= F; q++) Aug[r][q] -= fct * Aug[c][q]; }
        }
        double[] w = new double[F]; for (int f = 0; f < F; f++) w[f] = Aug[f][F] / Aug[f][f];
        double res = ss; for (int f = 0; f < F; f++) { res -= 2 * w[f] * Xty[f]; for (int g = 0; g < F; g++) res += w[f] * w[g] * (g >= f ? XtX[f][g] : XtX[g][f]); }
        double r2 = ss > 0 ? 1 - res / ss : 0;
        Integer[] ord = new Integer[F]; double[] score = new double[F];
        for (int f = 0; f < F; f++) { ord[f] = f; double mu = sx[f] / nx, sd = Math.sqrt(Math.max(0, sxx[f] / nx - mu * mu)); score[f] = Math.abs(w[f]) * sd; }
        Arrays.sort(ord, (a, b) -> Double.compare(score[b], score[a]));
        int kk = Math.min(k, F); int[] at = new int[kk]; double[] ww = new double[kk];
        for (int i = 0; i < kk; i++) { at[i] = cand.get(ord[i]); ww[i] = w[ord[i]]; }
        return new Distilled(at, ww, r2);
    }

    Ranked distillChild(Ranked parentR, Eras e) {
        ModelGenomeSU2 g = parentR.g; int end = Math.min(e.valEnd, lib.draws().length);
        Distilled d = distillCache.computeIfAbsent(tableKey(g) + "@" + end, k -> distill(table(g), lib.draws(), distillCandidates(curState), end, DISTILL_K));
        if (d == null) return null;
        List<Integer> atoms = new ArrayList<>(); for (int a : d.atoms) atoms.add(a);
        ModelGenomeSU2 child = new ModelGenomeSU2(Collections.singletonList(atomNet(atoms, new ArrayList<>(), new ArrayList<>(), false, 0.02, 1e-4, 1.0)), "STATIC", false, g.gate, g.coverage, g.metaC, true);
        StringBuilder note = new StringBuilder("DISTILL|PARENT_R2=").append(String.format(Locale.ROOT, "%.3f", d.r2)).append("|ATOMS=");
        for (int i = 0; i < Math.min(4, d.atoms.length); i++) note.append(i > 0 ? "," : "").append(AtomStreamSU2.CATALOG[d.atoms[i]].name()).append(String.format(Locale.ROOT, "(%+.3f)", d.weights[i]));
        return new Ranked(child, "distill", g.key(), note.append("|CHILD_REBUILT_FROM_PARENT_ATOMS").toString());
    }

    static List<Integer> neighbours(int i) {
        String fam = AtomStreamSU2.CATALOG[i].family; List<Integer> same = new ArrayList<>();
        for (int k = 0; k < AtomStreamSU2.size(); k++) if (AtomStreamSU2.CATALOG[k].family.equals(fam)) same.add(k);
        int j = same.indexOf(i); List<Integer> out = new ArrayList<>();
        for (int d : new int[]{-2, -1, 1, 2}) if (j + d >= 0 && j + d < same.size()) out.add(same.get(j + d));
        return out;
    }
    String lastNote = "";
    /** Children of an ATOMNET member follow the parent's learned credit (atomnet credit-guided edits, foundry14._credit_child). */
    ModelGenomeSU2.Member creditChild(ModelGenomeSU2.Member m) {
        Map<String, String> p = m.params;
        List<Integer> atoms = new ArrayList<>(); for (int x : AtomNetSU2.ints(p.get("atoms"))) atoms.add(x);
        List<int[]> pairs = new ArrayList<>(Arrays.asList(AtomNetSU2.pairList(p.get("pairs"))));
        List<Integer> lags = new ArrayList<>(); for (int x : AtomNetSU2.ints(p.get("lags"))) lags.add(x);
        boolean group = "1".equals(p.get("group")); double lr = Double.parseDouble(p.get("lr")), l2 = Double.parseDouble(p.get("l2")), gd = Double.parseDouble(p.get("gd"));
        List<int[]> mols = new ArrayList<>(Arrays.asList(AtomNetSU2.molList(p.get("mols"))));
        Map<String, Double> cr = credit(m); lastNote = "";
        double r0 = rng.nextDouble();
        if (r15 && cr != null && r0 < 0.25) {
            if (!mols.isEmpty() && rng.nextDouble() < 0.4) {
                int[] d = mols.get(0); double dv = Double.POSITIVE_INFINITY;
                for (int[] x : mols) { double v = cr.getOrDefault("M" + x[0] + "@" + x[1], 0.0); if (v < dv) { dv = v; d = x; } }
                mols.remove(d); lastNote = "MOLECULE_" + AtomStreamSU2.CATALOG[d[0]].name() + "|" + AtomStreamSU2.STATES[d[1]].name() + "_LOWEST_CREDIT|CHILD_DROPS_IT";
            } else if (!atoms.isEmpty() && mols.size() < 4) {
                int top = atoms.get(0); double tv = -1; for (int a : atoms) { double v = cr.getOrDefault("A" + a, 0.0); if (v > tv) { tv = v; top = a; } }
                int j = rng.nextInt(AtomStreamSU2.states()); mols.add(new int[]{top, j});
                lastNote = "ATOM_" + AtomStreamSU2.CATALOG[top].name() + "_HIGHEST_CREDIT|CHILD_MAKES_IT_STATE_DEPENDENT_ON_" + AtomStreamSU2.STATES[j].name();
            }
            return atomNet(atoms, pairs, lags, group, lr, l2, gd, mols);
        }
        if (cr != null && r0 < 0.75) {
            List<Integer> ranked = new ArrayList<>(atoms); ranked.sort(Comparator.comparingDouble((Integer i) -> cr.getOrDefault("A" + i, 0.0)).thenComparing(i -> i));
            double[] vals = atoms.stream().mapToDouble(i -> cr.getOrDefault("A" + i, 0.0)).sorted().toArray();
            double med = vals.length == 0 ? 0 : (vals.length % 2 == 1 ? vals[vals.length / 2] : (vals[vals.length / 2 - 1] + vals[vals.length / 2]) / 2);
            double q = rng.nextDouble();
            if (q < 0.3 && atoms.size() > 3) {
                int d = ranked.get(0); atoms.remove((Integer) d); pairs.removeIf(x -> x[0] == d || x[1] == d);
                lastNote = "ATOM_" + AtomStreamSU2.CATALOG[d].name() + "_LOWEST_CREDIT=" + String.format(Locale.ROOT, "%.3f", cr.getOrDefault("A" + d, 0.0)) + "|CHILD_DROPS_IT";
            } else if (q < 0.6 && !ranked.isEmpty()) {
                int top = ranked.get(ranked.size() - 1); List<Integer> nb = new ArrayList<>(); for (int k : neighbours(top)) if (!atoms.contains(k)) nb.add(k);
                if (!nb.isEmpty()) { int k = nb.get(rng.nextInt(nb.size())); atoms.add(k); lastNote = "ATOM_" + AtomStreamSU2.CATALOG[top].name() + "_HIGHEST_CREDIT|CHILD_EXPLORES_" + AtomStreamSU2.CATALOG[k].name(); }
            } else if (q < 0.8 && ranked.size() >= 2 && pairs.size() < 3) {
                int i = ranked.get(ranked.size() - 1), j = ranked.get(ranked.size() - 2);
                boolean has = false; for (int[] x : pairs) has |= Math.min(x[0], x[1]) == Math.min(i, j) && Math.max(x[0], x[1]) == Math.max(i, j);
                if (!has) { pairs.add(new int[]{Math.min(i, j), Math.max(i, j)}); lastNote = "TWO_STRONGEST_ATOMS_CROSSED|" + AtomStreamSU2.CATALOG[i].name() + "x" + AtomStreamSU2.CATALOG[j].name(); }
            } else {
                Double gc = cr.get("GROUP"), rc = cr.get("REL");
                if (gc != null && gc < 0.5 * med) { group = false; lastNote = "GROUP_ATOMS_LOW_CREDIT|CHILD_DROPS_THEM"; }
                else if (rc != null && rc < med) { lags.clear(); lastNote = "RELATION_ATOMS_LOW_CREDIT|CHILD_DROPS_THEM"; }
                else if (!group) { group = true; lastNote = "CHILD_TRIES_GROUP_ATOMS"; }
                else { int l = rng.nextBoolean() ? 1 : 2; if (!lags.contains(l)) lags.add(l); lastNote = "CHILD_TRIES_RELATION_ATOMS"; }
            }
        } else {
            double q = rng.nextDouble();
            if (q < 0.3) atoms.add(r15 ? priorPick(atoms) : rng.nextInt(universe()));
            else if (q < 0.5 && atoms.size() > 2) { int d = atoms.get(rng.nextInt(atoms.size())); atoms.remove((Integer) d); pairs.removeIf(x -> x[0] == d || x[1] == d); }
            else if (q < 0.7) lr = new double[]{0.02, 0.05, 0.1}[rng.nextInt(3)];
            else if (q < 0.85) gd = new double[]{1.0, 0.999, 0.995}[rng.nextInt(3)];
            else l2 = new double[]{1e-4, 1e-3}[rng.nextInt(2)];
        }
        if (atoms.isEmpty()) atoms.add(rng.nextInt(universe()));
        return atomNet(atoms, pairs, lags, group, lr, l2, gd, mols);
    }

    /** Open relation search (P41): add, drop or move one (source offset, lag) pair of a LOGLIN member. */
    ModelGenomeSU2.Member loglinChild(ModelGenomeSU2.Member m) {
        int[][] src = ModelReplaySU2.pairs(m.params.get("sources")); List<int[]> s = new ArrayList<>(); for (int[] x : src) s.add(x.clone());
        double q = rng.nextDouble();
        if (q < 0.35 && s.size() < 5) { int[] n = {rng.nextInt(10), new int[]{1, 1, 2, 3}[rng.nextInt(4)]}; if (indexOf(s, n) < 0) s.add(n); }
        else if (q < 0.6 && s.size() > 1) s.remove(rng.nextInt(s.size()));
        else { int i = rng.nextInt(s.size()); int[] o = s.get(i); int[] n = rng.nextDouble() < 0.6 ? new int[]{(o[0] + (rng.nextBoolean() ? 1 : 9)) % 10, o[1]} : new int[]{o[0], Math.min(3, Math.max(1, o[1] + (rng.nextBoolean() ? 1 : -1)))}; if (indexOf(s, n) < 0) s.set(i, n); }
        Map<String, String> p = new TreeMap<>(m.params); p.put("sources", sourcesText(s));
        return new ModelGenomeSU2.Member(m.kind, p, m.weight);
    }
    /** RELMEM (P48): open relation search on its arbitrary-map (offset, lag) sources (foundry14._relmem_child). */
    ModelGenomeSU2.Member relmemChild(ModelGenomeSU2.Member m) {
        List<int[]> s = new ArrayList<>(); for (int[] x : ModelReplaySU2.pairsOrNone(m.params.get("sources"))) s.add(x.clone());
        double q = rng.nextDouble();
        if ((q < 0.4 || s.isEmpty()) && s.size() < 4) { int[] n = {rng.nextInt(10), new int[]{1, 1, 2, 3}[rng.nextInt(4)]}; if (indexOf(s, n) < 0) s.add(n); }
        else if (q < 0.65) s.remove(rng.nextInt(s.size()));
        else { int i = rng.nextInt(s.size()); int[] o = s.get(i); int[] n = rng.nextDouble() < 0.6 ? new int[]{(o[0] + (rng.nextBoolean() ? 1 : 9)) % 10, o[1]} : new int[]{o[0], Math.min(3, Math.max(1, o[1] + (rng.nextBoolean() ? 1 : -1)))}; if (indexOf(s, n) < 0) s.set(i, n); }
        Map<String, String> p = new TreeMap<>(m.params); p.put("sources", sourcesText(s));
        return new ModelGenomeSU2.Member(m.kind, p, m.weight);
    }
    static int indexOf(List<int[]> s, int[] x) { for (int i = 0; i < s.size(); i++) if (s.get(i)[0] == x[0] && s.get(i)[1] == x[1]) return i; return -1; }
    /** Python tuple text: ((o, l),) or ((o, l), (o, l)) */
    static String sourcesText(List<int[]> s) {
        StringBuilder b = new StringBuilder("(");
        for (int i = 0; i < s.size(); i++) { if (i > 0) b.append(", "); b.append('(').append(s.get(i)[0]).append(", ").append(s.get(i)[1]).append(')'); }
        return b.append(s.size() == 1 ? ",)" : ")").toString();
    }

    static double tierCoverage(String t) { return t.equals("F15") ? 0.15 : t.equals("F30") ? 0.30 : 1.0; }

    // ---------------------------------------------------------------- genomes
    Map<String, String> randomParams(String kind) { Map<String, String> p = new TreeMap<>(); for (Map.Entry<String, String[]> e : GRID.get(kind).entrySet()) p.put(e.getKey(), e.getValue()[rng.nextInt(e.getValue().length)]); return p; }
    static Map<String, String> centralParams(String kind) { Map<String, String> p = new TreeMap<>(); for (Map.Entry<String, String[]> e : GRID.get(kind).entrySet()) p.put(e.getKey(), e.getValue()[e.getValue().length / 2]); return p; }
    ModelGenomeSU2.Member randomMember(String kind) {
        if (kind.equals("ATOMNET")) return randomAtomNet();
        ModelGenomeSU2.Member m = new ModelGenomeSU2.Member(kind, randomParams(kind), 1.0);
        if (r14b && kind.equals("LOGLIN") && rng.nextDouble() < 0.5) {
            int k = 1 + rng.nextInt(4); List<int[]> s = new ArrayList<>();
            while (s.size() < k) { int[] n = {rng.nextInt(10), new int[]{1, 1, 2, 3}[rng.nextInt(4)]}; if (indexOf(s, n) < 0) s.add(n); }
            Map<String, String> p = new TreeMap<>(m.params); p.put("sources", sourcesText(s)); m = new ModelGenomeSU2.Member(kind, p, 1.0);
        }
        return m;
    }
    String[] kinds() {
        if (!r14b) return ModelGenomeSU2.KINDS;
        List<String> k = new ArrayList<>(Arrays.asList(ModelGenomeSU2.KINDS));
        if (r16) k.addAll(Arrays.asList(ModelGenomeSU2.R16_KINDS));
        k.add("ATOMNET");
        return k.toArray(new String[0]);
    }
    String randomKind() { String[] k = kinds(); return k[rng.nextInt(k.length)]; }
    double randomCoverage() { return ModelGenomeSU2.COVERAGES[rng.nextInt(ModelGenomeSU2.COVERAGES.length)]; }

    String randomPool() {
        if (!r15) return new String[]{"STATIC", "BMA", "GATED"}[rng.nextInt(3)];
        String p = new String[]{"STATIC", "BMA", "GATED", "TEACHER"}[rng.nextInt(4)];
        return p.equals("TEACHER") ? "TEACHER:" + rng.nextInt(AtomStreamSU2.states()) : p;
    }
    public ModelGenomeSU2 randomGenome(String kind) {
        List<ModelGenomeSU2.Member> m = new ArrayList<>(); m.add(randomMember(kind));
        if (rng.nextDouble() < .35) m.add(randomMember(randomKind()));
        return new ModelGenomeSU2(dedupe(m), randomPool(), rng.nextDouble() < .3, rng.nextBoolean() ? "MASS" : "META", randomCoverage(), 0.2, r14);
    }

    public static List<ModelGenomeSU2> screenSeeds() { return screenSeeds(false); }
    public static List<ModelGenomeSU2> screenSeeds(boolean calibrated) {
        List<ModelGenomeSU2> out = new ArrayList<>();
        for (String kind : ModelGenomeSU2.KINDS) for (String gate : new String[]{"MASS", "META"}) for (double cov : new double[]{0.15, 0.30})
            out.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind, centralParams(kind), 1.0)), "STATIC", false, gate, cov, 0.2, calibrated));
        return out;
    }

    static List<ModelGenomeSU2.Member> dedupe(List<ModelGenomeSU2.Member> in) {
        LinkedHashMap<String, ModelGenomeSU2.Member> m = new LinkedHashMap<>(); for (ModelGenomeSU2.Member x : in) m.putIfAbsent(x.modelKey(), x);
        return new ArrayList<>(m.values()).subList(0, Math.min(4, m.size()));
    }

    ModelGenomeSU2.Member jitter(ModelGenomeSU2.Member x) {
        if (x.kind.equals("ATOMNET")) return creditChild(x);
        if (r14b && x.kind.equals("LOGLIN") && rng.nextDouble() < 0.6) return loglinChild(x);
        if (r16 && x.kind.equals("RELMEM") && rng.nextDouble() < 0.5) return relmemChild(x);
        if (!GRID.containsKey(x.kind)) return x;
        Map<String, String> p = new TreeMap<>(x.params);
        List<String> keys = new ArrayList<>(GRID.get(x.kind).keySet()); String k = keys.get(rng.nextInt(keys.size()));
        String[] vals = GRID.get(x.kind).get(k); int i = Math.max(0, Arrays.asList(vals).indexOf(p.get(k)));
        if (vals.length > 1) i = Math.max(0, Math.min(vals.length - 1, i + (rng.nextBoolean() ? 1 : -1)));
        p.put(k, vals[i]);
        return new ModelGenomeSU2.Member(x.kind, p, x.weight);
    }

    /** Feedback analysis (P22): the state in which the parent loses most, on DEV settled rows only. */
    String[] diagnose(ModelGenomeSU2 g, Eras e) {
        ModelReplaySU2.Result r = eval(g);
        int nAct = r.actions(e.devStart, e.devEnd);
        double base = nAct > 0 ? r.hits(e.devStart, e.devEnd) / (double) nAct : 0.3;
        double worstGap = Double.POSITIVE_INFINITY; String[] worst = null;
        for (String kind : r16 ? new String[]{"BOCPD", "HMM", "MSDIR", "RELBAYES", "REGMEM", "RELMEM"} : new String[]{"BOCPD", "HMM", "MSDIR", "RELBAYES"}) {
            float[][] st = lib.get(new ModelGenomeSU2.Member(kind, centralParams(kind), 1.0)).state;
            int n = e.devEnd - e.devStart; double[] v = new double[n];
            for (int t = 0; t < n; t++) v[t] = st[e.devStart + t][0];
            double[] sorted = v.clone(); Arrays.sort(sorted);
            double q1 = ModelReplaySU2.quantileLinear(sorted.clone(), 1 / 3.0), q2 = ModelReplaySU2.quantileLinear(sorted.clone(), 2 / 3.0);
            for (int side = 0; side < 2; side++) {
                int cnt = 0, hit = 0;
                for (int t = 0; t < n; t++) {
                    boolean sel = side == 0 ? v[t] <= q1 : v[t] >= q2; int u = e.devStart + t;
                    if (sel && (nAct > 150 ? r.act[u] : true)) { cnt++; if (r.hit[u]) hit++; }
                }
                if (cnt < 60) continue;
                double rate = hit / (double) cnt, gap = rate - base;
                if (gap < worstGap) { worstGap = gap; worst = new String[]{kind, side == 0 ? "LOW" : "HIGH", String.format(Locale.ROOT, "%.4f", rate), String.valueOf(cnt), String.format(Locale.ROOT, "%+.4f", gap)}; }
            }
        }
        return worst;
    }

    Ranked apply(String op, Ranked parentR, List<Ranked> hof, Eras e) {
        if (op.equals("distill") && r15) { Ranked c = distillChild(parentR, e); if (c != null) return c; op = "mutate"; }
        ModelGenomeSU2 g = parentR.g; List<ModelGenomeSU2.Member> mem = new ArrayList<>(g.members);
        String pool = g.pool, gate = g.gate; boolean sink = g.sink; double cov = g.coverage, c = g.metaC; String note = "";
        switch (op) {
            case "credit": {
                int ai = -1; for (int i = 0; i < mem.size(); i++) if (mem.get(i).kind.equals("ATOMNET")) ai = i;
                if (ai >= 0) { mem.set(ai, creditChild(mem.get(ai))); note = lastNote; break; }
                if (mem.size() < 4) { mem.add(defaultAtomNet()); note = "PARENT_HAS_NO_ATOM_NETWORK|CHILD_GRAFTS_DEFAULT_ATOMNET"; break; }
                op = "mutate";
            } // fallthrough
            case "add": if (mem.size() < 4 && !op.equals("mutate")) { mem.add(randomMember(randomKind())); break; } op = "mutate"; // fallthrough
            case "mutate": { int i = rng.nextInt(mem.size()); lastNote = ""; mem.set(i, jitter(mem.get(i))); note = lastNote; break; }
            case "remove": if (mem.size() > 1) mem.remove(rng.nextInt(mem.size())); else { int i = rng.nextInt(mem.size()); mem.set(i, jitter(mem.get(i))); } break;
            case "reweight": if (mem.size() > 1) { int i = rng.nextInt(mem.size()); ModelGenomeSU2.Member x = mem.get(i); double w = Math.max(0.1, Math.min(3.0, x.weight * Math.exp(rng.nextGaussian() * 0.4))); mem.set(i, new ModelGenomeSU2.Member(x.kind, x.params, Math.round(w * 100) / 100.0)); } break;
            case "pool": { String np; do { np = randomPool(); } while (np.equals(pool)); pool = np; if (rng.nextDouble() < .4) sink = !sink;
                if (TeacherPoolSU2.isTeacher(pool)) note = "CHILD_USES_TEACHER_MATRIX_BY_STATE_" + AtomStreamSU2.STATES[TeacherPoolSU2.stateOf(pool)].name(); break; }
            case "gate": if (gate.equals("MASS")) gate = "META"; else c = new double[]{0.05, 0.1, 0.2, 0.5}[rng.nextInt(4)]; break;
            case "coverage": { double nc; do { nc = randomCoverage(); } while (nc == cov); cov = nc; break; }
            case "crossover": if (!hof.isEmpty()) { Ranked o = hof.get(rng.nextInt(hof.size())); List<ModelGenomeSU2.Member> u = new ArrayList<>(g.members); u.addAll(o.g.members); Collections.shuffle(u, rng); int k = Math.max(1, Math.min(4, (g.members.size() + o.g.members.size() + 1) / 2)); mem = dedupe(u.subList(0, Math.min(k, u.size()))); } break;
            case "fusion": if (!hof.isEmpty() && mem.size() < 4) { Ranked o = hof.get(rng.nextInt(hof.size())); mem.add(o.g.members.get(rng.nextInt(o.g.members.size()))); pool = r15 ? randomPool() : new String[]{"STATIC", "BMA", "GATED"}[rng.nextInt(3)]; } break;
            case "diagnose": {
                String[] w = diagnose(g, e);
                if (w != null) {
                    boolean has = false; for (ModelGenomeSU2.Member m : mem) has |= m.kind.equals(w[0]);
                    if (!has && mem.size() < 4) mem.add(randomMember(w[0]));
                    gate = "META";
                    note = "PARENT_LOSES_WHEN_" + w[0] + "_STATE_" + w[1] + "|rate=" + w[2] + "|n=" + w[3] + "|gap=" + w[4] + "|CHILD_ADDS_" + w[0] + "_AND_APPLICABILITY_GATE";
                }
                break;
            }
            case "prune": {
                // effective-complexity audit: drop the member whose removal costs nothing on DEV
                if (mem.size() > 1) {
                    double base = dev(g, e), bestF = Double.NEGATIVE_INFINITY; int bestI = -1;
                    for (int i = 0; i < mem.size(); i++) {
                        List<ModelGenomeSU2.Member> less = new ArrayList<>(mem); less.remove(i);
                        double f = dev(new ModelGenomeSU2(less, pool, sink, gate, cov, c, g.calibrated), e);
                        if (f >= base - 1e-4 && f > bestF) { bestF = f; bestI = i; }
                    }
                    if (bestI >= 0) { note = "MEMBER_" + mem.get(bestI).kind + "_HAS_NO_DEV_CONTRIBUTION|without=" + String.format(Locale.ROOT, "%.4f", bestF) + "|with=" + String.format(Locale.ROOT, "%.4f", base) + "|CHILD_REMOVES_IT"; mem.remove(bestI); }
                }
                break;
            }
            default: { int i = rng.nextInt(mem.size()); mem.set(i, jitter(mem.get(i))); }
        }
        return new Ranked(new ModelGenomeSU2(dedupe(mem), pool, sink, gate, cov, c, g.calibrated), op, g.key(), note);
    }

    static boolean subsHasText(List<ModelGenomeSU2> subs, ModelGenomeSU2 a) { for (ModelGenomeSU2 x : subs) if (x.text().equals(a.text())) return true; return false; }

    String pickOp(State s) {
        String[] ops = r15 ? OPS_R15 : r14b ? OPS_R14B : OPS;
        double[] w = new double[ops.length]; double z = 0;
        for (int i = 0; i < ops.length; i++) { double[] v = s.opStats.getOrDefault(ops[i], new double[]{1, 1}); w[i] = v[1] / v[0] + 0.05; z += w[i]; }
        double u = rng.nextDouble() * z; for (int i = 0; i < ops.length; i++) { u -= w[i]; if (u <= 0) return ops[i]; } return ops[ops.length - 1];
    }
    String pickKind(State s, int total) {
        String best = null; double bv = Double.NEGATIVE_INFINITY;
        for (String k : kinds()) { double[] v = s.kindStats.getOrDefault(k, new double[]{1, 0}); double u = v[1] / v[0] + 0.01 * Math.sqrt(2 * Math.log(total + 2) / v[0]) + rng.nextDouble() * 1e-6; if (u > bv) { bv = u; best = k; } }
        return best;
    }

    /** One campaign; returns the continued state (never a restart). */
    public State campaign(State s, Eras e, int generations, int population, int hofSize) {
        devFit.clear(); valFit.clear();                                  // eras moved: everything is re-scored
        curEras = e;                                                     // credit-guided children read weights from draws < valEnd only
        curState = s;
        if (s.population.isEmpty()) {
            for (ModelGenomeSU2 g : screenSeeds(r14)) s.population.add(new Ranked(g, "screen", "", ""));
            if (r14b) {
                for (Object[] gc : new Object[][]{{"MASS", 0.15}, {"MASS", 0.30}, {"META", 0.15}})
                    s.population.add(new Ranked(new ModelGenomeSU2(Collections.singletonList(defaultAtomNet()), "STATIC", false, (String) gc[0], (Double) gc[1], 0.2, true), "screen", "", ""));
                List<Integer> a = new ArrayList<>(); for (int x : defaultAtoms()) a.add(x);
                s.population.add(new Ranked(new ModelGenomeSU2(Collections.singletonList(atomNet(a, new ArrayList<>(), Collections.singletonList(1), true, 0.05, 1e-4, 0.999)), "STATIC", false, "MASS", 0.15, 0.2, true), "screen", "", ""));
            }
            if (r16)                                                      // regime-memory families at their central parameters
                for (String kind : ModelGenomeSU2.R16_KINDS) for (String gate : new String[]{"MASS", "META"}) for (double cov : new double[]{0.15, 0.30})
                    s.population.add(new Ranked(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind, centralParams(kind), 1.0)), "STATIC", false, gate, cov, 0.2, true), "screen:" + kind, "", ""));
            if (r15) {                                                    // the user's XI method decomposed into its atoms
                ModelGenomeSU2.Member xi = atomNet(xiAtoms(), new ArrayList<>(), new ArrayList<>(), false, 0.02, 1e-4, 1.0);
                for (Object[] gc : new Object[][]{{"MASS", 0.15}, {"MASS", 0.30}, {"META", 0.15}})
                    s.population.add(new Ranked(new ModelGenomeSU2(Collections.singletonList(xi), "STATIC", false, (String) gc[0], (Double) gc[1], 0.2, true), "screen:XI_DECOMPOSED", "", ""));
            }
        }
        String[] kinds = kinds();
        while (s.population.size() < population) s.population.add(new Ranked(randomGenome(kinds[s.population.size() % kinds.length]), "random", "", ""));
        double champVal = s.champion == null ? -9 : val(s.champion.g, e);
        Map<String, Double> tierVal = new TreeMap<>(); for (Map.Entry<String, Ranked> t : s.tiers.entrySet()) tierVal.put(t.getKey(), val(t.getValue().g, e));
        int evaluated = 0;
        for (int gen = 0; gen < generations; gen++) {
            if (r14) {                                                    // consensus of the central models and the hall of fame
                List<ModelGenomeSU2> progs = centralPrograms(r14b, r16);
                Map<String, Integer> perFamily = new HashMap<>();
                for (int i = 0; i < Math.min(hofSize, s.hallOfFame.size()); i++) {
                    ModelGenomeSU2 h = s.hallOfFame.get(i).g;
                    if (r14b) {                                           // R14b: at most 2 programs per family signature
                        TreeSet<String> fam = new TreeSet<>(); for (ModelGenomeSU2.Member m : h.members) fam.add(m.kind);
                        int c0 = perFamily.merge(String.join("+", fam), 1, Integer::sum); if (c0 > 2) continue;
                    }
                    progs.add(h);
                }
                buildConsensus(progs); devFit.clear(); valFit.clear();
            }
            List<Ranked> scored = new ArrayList<>(s.population);
            scored.sort(Comparator.comparingDouble((Ranked r) -> -dev(r.g, e)).thenComparing(r -> r.g.key()));
            Set<String> inHof = new HashSet<>(); for (Ranked h : s.hallOfFame) inHof.add(h.g.key());
            for (int i = 0; i < Math.min(6, scored.size()); i++) if (inHof.add(scored.get(i).g.key())) s.hallOfFame.add(scored.get(i));
            s.hallOfFame.sort(Comparator.comparingDouble((Ranked r) -> -dev(r.g, e)).thenComparing(r -> r.g.key()));
            while (s.hallOfFame.size() > hofSize) s.hallOfFame.remove(s.hallOfFame.size() - 1);
            if (r15) updatePrior(s);
            LinkedHashMap<String, Ranked> cands = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(8, scored.size()); i++) cands.putIfAbsent(scored.get(i).g.key(), scored.get(i));
            for (String t : TIERS) { int k = 0; for (Ranked r : scored) if (tierOf(r.g).equals(t) && k++ < 3) cands.putIfAbsent(r.g.key(), r); }
            Ranked bestC = null; double bestV = Double.NEGATIVE_INFINITY;
            for (Ranked r : cands.values()) {
                double v = val(r.g, e);
                if (v > bestV) { bestV = v; bestC = r; }
                String t = tierOf(r.g); if (v > tierVal.getOrDefault(t, -9.0)) { tierVal.put(t, v); s.tiers.put(t, r); }
            }
            if (r14) {
                // committees per tier: the top programs by the judge at that tier's frequency, distinct tables only
                LinkedHashMap<String, Ranked> pool = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(30, scored.size()); i++) pool.putIfAbsent(scored.get(i).g.key(), scored.get(i));
                for (Ranked h : s.hallOfFame) pool.putIfAbsent(h.g.key(), h);
                for (String t : TIERS) {
                    double tc = tierCoverage(t); List<Object[]> ranked = new ArrayList<>();
                    for (Ranked r : pool.values()) { ModelGenomeSU2 a = r.g.asTier(tc); ranked.add(new Object[]{val(a, e), a}); }
                    ranked.sort((x, y) -> { int c0 = Double.compare((Double) y[0], (Double) x[0]); return c0 != 0 ? c0 : ((ModelGenomeSU2) x[1]).key().compareTo(((ModelGenomeSU2) y[1]).key()); });
                    List<ModelGenomeSU2> subs = new ArrayList<>(); Set<String> tk = new HashSet<>();
                    for (Object[] x : ranked) { ModelGenomeSU2 a = (ModelGenomeSU2) x[1]; if (tk.add(tableKey(a)) && !subsHasText(subs, a)) subs.add(a); if (subs.size() == CommitteeSU2.MAX_MEMBERS) break; }
                    if (!subs.isEmpty()) s.committees.put(t, new CommitteeSU2(subs, tc));
                }
                double cj = s.committees.containsKey("F15") ? judge(CommitteeSU2.evaluate(s.committees.get("F15"), lib, startMeta), e.devStart, e.valEnd, 4) : -9;
                if (cj > s.committeeJudge) { s.committeeJudge = cj; s.effectiveGenerations++; }
            }
            if (bestV > champVal) { champVal = bestV; s.champion = bestC; if (!r14) s.effectiveGenerations++; }
            int eliteN = Math.max(4, population / 4);
            List<Ranked> next = new ArrayList<>(scored.subList(0, Math.min(eliteN, scored.size())));
            List<Ranked> parents = new ArrayList<>(next); parents.addAll(s.hallOfFame);
            Set<String> keys = new HashSet<>(); for (Ranked r : next) keys.add(r.g.key());
            while (next.size() < population) {
                String op = pickOp(s); Ranked parent;
                if (rng.nextDouble() < .2) { parent = new Ranked(randomGenome(pickKind(s, evaluated)), "random", "", ""); op = "mutate"; }
                else parent = parents.get(rng.nextInt(parents.size()));
                Ranked child = apply(op, parent, s.hallOfFame, e);
                if (!keys.add(child.g.key())) { child = apply("mutate", child, s.hallOfFame, e); keys.add(child.g.key()); }
                double pf = dev(parent.g, e), cf = dev(child.g, e); evaluated++;
                double[] os = s.opStats.computeIfAbsent(op, k -> new double[]{1, 1}); os[0]++; if (cf > pf) os[1]++;
                for (ModelGenomeSU2.Member m : child.g.members) { double[] ks = s.kindStats.computeIfAbsent(m.kind, k -> new double[]{1, 0}); ks[0]++; ks[1] += Math.max(0, cf - pf); }
                if (!child.note.isEmpty() && s.repairNotes.size() < 200) s.repairNotes.add(child.note + "|parentDev=" + String.format(Locale.ROOT, "%.4f", pf) + "|childDev=" + String.format(Locale.ROOT, "%.4f", cf));
                next.add(child);
            }
            s.population.clear(); s.population.addAll(next);
        }
        return s;
    }
}
