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
    }

    final ModelReplaySU2.Library lib; final Random rng; final int startMeta, window, refit;
    final Map<String, Double> devFit = new HashMap<>(), valFit = new HashMap<>();
    final Map<String, ModelReplaySU2.Result> results = new LinkedHashMap<String, ModelReplaySU2.Result>(64, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, ModelReplaySU2.Result> e) { return size() > 400; }
    };
    public ModelEvolutionSU2(ModelReplaySU2.Library lib, long seed, int startMeta, int window, int refit) { this.lib = lib; rng = new Random(seed); this.startMeta = startMeta; this.window = window; this.refit = refit; }

    public static String tierOf(ModelGenomeSU2 g) { return g.coverage >= .999 ? "F100" : g.coverage >= .30 ? "F30" : "F15"; }

    ModelReplaySU2.Result eval(ModelGenomeSU2 g) { return results.computeIfAbsent(g.key(), k -> ModelReplaySU2.evaluate(g, lib, startMeta, window, refit)); }
    double dev(ModelGenomeSU2 g, Eras e) { return devFit.computeIfAbsent(g.key(), k -> ModelReplaySU2.fitness(eval(g), e.devStart, e.devEnd, 3, 0.12, 200)); }
    /** R13 v2: champion/tier choice on all draws before the checkpoint (DEV+VAL, 4 folds); VAL alone carried no forward signal. */
    double val(ModelGenomeSU2 g, Eras e) { return valFit.computeIfAbsent(g.key(), k -> ModelReplaySU2.fitness(eval(g), e.devStart, e.valEnd, 4, 0.12, 200)); }

    // ---------------------------------------------------------------- genomes
    Map<String, String> randomParams(String kind) { Map<String, String> p = new TreeMap<>(); for (Map.Entry<String, String[]> e : GRID.get(kind).entrySet()) p.put(e.getKey(), e.getValue()[rng.nextInt(e.getValue().length)]); return p; }
    static Map<String, String> centralParams(String kind) { Map<String, String> p = new TreeMap<>(); for (Map.Entry<String, String[]> e : GRID.get(kind).entrySet()) p.put(e.getKey(), e.getValue()[e.getValue().length / 2]); return p; }
    ModelGenomeSU2.Member randomMember(String kind) { return new ModelGenomeSU2.Member(kind, randomParams(kind), 1.0); }
    String randomKind() { return ModelGenomeSU2.KINDS[rng.nextInt(ModelGenomeSU2.KINDS.length)]; }
    double randomCoverage() { return ModelGenomeSU2.COVERAGES[rng.nextInt(ModelGenomeSU2.COVERAGES.length)]; }

    public ModelGenomeSU2 randomGenome(String kind) {
        List<ModelGenomeSU2.Member> m = new ArrayList<>(); m.add(randomMember(kind));
        if (rng.nextDouble() < .35) m.add(randomMember(randomKind()));
        return new ModelGenomeSU2(dedupe(m), new String[]{"STATIC", "BMA", "GATED"}[rng.nextInt(3)], rng.nextDouble() < .3, rng.nextBoolean() ? "MASS" : "META", randomCoverage(), 0.2);
    }

    public static List<ModelGenomeSU2> screenSeeds() {
        List<ModelGenomeSU2> out = new ArrayList<>();
        for (String kind : ModelGenomeSU2.KINDS) for (String gate : new String[]{"MASS", "META"}) for (double cov : new double[]{0.15, 0.30})
            out.add(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind, centralParams(kind), 1.0)), "STATIC", false, gate, cov, 0.2));
        return out;
    }

    static List<ModelGenomeSU2.Member> dedupe(List<ModelGenomeSU2.Member> in) {
        LinkedHashMap<String, ModelGenomeSU2.Member> m = new LinkedHashMap<>(); for (ModelGenomeSU2.Member x : in) m.putIfAbsent(x.modelKey(), x);
        return new ArrayList<>(m.values()).subList(0, Math.min(4, m.size()));
    }

    ModelGenomeSU2.Member jitter(ModelGenomeSU2.Member x) {
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
        for (String kind : new String[]{"BOCPD", "HMM", "MSDIR", "RELBAYES"}) {
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
        ModelGenomeSU2 g = parentR.g; List<ModelGenomeSU2.Member> mem = new ArrayList<>(g.members);
        String pool = g.pool, gate = g.gate; boolean sink = g.sink; double cov = g.coverage, c = g.metaC; String note = "";
        switch (op) {
            case "add": if (mem.size() < 4) { mem.add(randomMember(randomKind())); break; } op = "mutate"; // fallthrough
            case "mutate": { int i = rng.nextInt(mem.size()); mem.set(i, jitter(mem.get(i))); break; }
            case "remove": if (mem.size() > 1) mem.remove(rng.nextInt(mem.size())); else { int i = rng.nextInt(mem.size()); mem.set(i, jitter(mem.get(i))); } break;
            case "reweight": if (mem.size() > 1) { int i = rng.nextInt(mem.size()); ModelGenomeSU2.Member x = mem.get(i); double w = Math.max(0.1, Math.min(3.0, x.weight * Math.exp(rng.nextGaussian() * 0.4))); mem.set(i, new ModelGenomeSU2.Member(x.kind, x.params, Math.round(w * 100) / 100.0)); } break;
            case "pool": { String[] pools = {"STATIC", "BMA", "GATED"}; String np; do { np = pools[rng.nextInt(3)]; } while (np.equals(pool)); pool = np; if (rng.nextDouble() < .4) sink = !sink; break; }
            case "gate": if (gate.equals("MASS")) gate = "META"; else c = new double[]{0.05, 0.1, 0.2, 0.5}[rng.nextInt(4)]; break;
            case "coverage": { double nc; do { nc = randomCoverage(); } while (nc == cov); cov = nc; break; }
            case "crossover": if (!hof.isEmpty()) { Ranked o = hof.get(rng.nextInt(hof.size())); List<ModelGenomeSU2.Member> u = new ArrayList<>(g.members); u.addAll(o.g.members); Collections.shuffle(u, rng); int k = Math.max(1, Math.min(4, (g.members.size() + o.g.members.size() + 1) / 2)); mem = dedupe(u.subList(0, Math.min(k, u.size()))); } break;
            case "fusion": if (!hof.isEmpty() && mem.size() < 4) { Ranked o = hof.get(rng.nextInt(hof.size())); mem.add(o.g.members.get(rng.nextInt(o.g.members.size()))); pool = new String[]{"STATIC", "BMA", "GATED"}[rng.nextInt(3)]; } break;
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
                        double f = dev(new ModelGenomeSU2(less, pool, sink, gate, cov, c), e);
                        if (f >= base - 1e-4 && f > bestF) { bestF = f; bestI = i; }
                    }
                    if (bestI >= 0) { note = "MEMBER_" + mem.get(bestI).kind + "_HAS_NO_DEV_CONTRIBUTION|without=" + String.format(Locale.ROOT, "%.4f", bestF) + "|with=" + String.format(Locale.ROOT, "%.4f", base) + "|CHILD_REMOVES_IT"; mem.remove(bestI); }
                }
                break;
            }
            default: { int i = rng.nextInt(mem.size()); mem.set(i, jitter(mem.get(i))); }
        }
        return new Ranked(new ModelGenomeSU2(dedupe(mem), pool, sink, gate, cov, c), op, g.key(), note);
    }

    String pickOp(State s) {
        double[] w = new double[OPS.length]; double z = 0;
        for (int i = 0; i < OPS.length; i++) { double[] v = s.opStats.getOrDefault(OPS[i], new double[]{1, 1}); w[i] = v[1] / v[0] + 0.05; z += w[i]; }
        double u = rng.nextDouble() * z; for (int i = 0; i < OPS.length; i++) { u -= w[i]; if (u <= 0) return OPS[i]; } return OPS[OPS.length - 1];
    }
    String pickKind(State s, int total) {
        String best = null; double bv = Double.NEGATIVE_INFINITY;
        for (String k : ModelGenomeSU2.KINDS) { double[] v = s.kindStats.getOrDefault(k, new double[]{1, 0}); double u = v[1] / v[0] + 0.01 * Math.sqrt(2 * Math.log(total + 2) / v[0]) + rng.nextDouble() * 1e-6; if (u > bv) { bv = u; best = k; } }
        return best;
    }

    /** One campaign; returns the continued state (never a restart). */
    public State campaign(State s, Eras e, int generations, int population, int hofSize) {
        devFit.clear(); valFit.clear();                                  // eras moved: everything is re-scored
        if (s.population.isEmpty()) for (ModelGenomeSU2 g : screenSeeds()) s.population.add(new Ranked(g, "screen", "", ""));
        while (s.population.size() < population) s.population.add(new Ranked(randomGenome(ModelGenomeSU2.KINDS[s.population.size() % ModelGenomeSU2.KINDS.length]), "random", "", ""));
        double champVal = s.champion == null ? -9 : val(s.champion.g, e);
        Map<String, Double> tierVal = new TreeMap<>(); for (Map.Entry<String, Ranked> t : s.tiers.entrySet()) tierVal.put(t.getKey(), val(t.getValue().g, e));
        int evaluated = 0;
        for (int gen = 0; gen < generations; gen++) {
            List<Ranked> scored = new ArrayList<>(s.population);
            scored.sort(Comparator.comparingDouble((Ranked r) -> -dev(r.g, e)).thenComparing(r -> r.g.key()));
            Set<String> inHof = new HashSet<>(); for (Ranked h : s.hallOfFame) inHof.add(h.g.key());
            for (int i = 0; i < Math.min(6, scored.size()); i++) if (inHof.add(scored.get(i).g.key())) s.hallOfFame.add(scored.get(i));
            s.hallOfFame.sort(Comparator.comparingDouble((Ranked r) -> -dev(r.g, e)).thenComparing(r -> r.g.key()));
            while (s.hallOfFame.size() > hofSize) s.hallOfFame.remove(s.hallOfFame.size() - 1);
            LinkedHashMap<String, Ranked> cands = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(8, scored.size()); i++) cands.putIfAbsent(scored.get(i).g.key(), scored.get(i));
            for (String t : TIERS) { int k = 0; for (Ranked r : scored) if (tierOf(r.g).equals(t) && k++ < 3) cands.putIfAbsent(r.g.key(), r); }
            Ranked bestC = null; double bestV = Double.NEGATIVE_INFINITY;
            for (Ranked r : cands.values()) {
                double v = val(r.g, e);
                if (v > bestV) { bestV = v; bestC = r; }
                String t = tierOf(r.g); if (v > tierVal.getOrDefault(t, -9.0)) { tierVal.put(t, v); s.tiers.put(t, r); }
            }
            if (bestV > champVal) { champVal = bestV; s.champion = bestC; s.effectiveGenerations++; }
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
