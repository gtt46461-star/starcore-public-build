package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** P49 deployment: an immutable fitted layer, causal frequency gate and ordinary frozen league evidence. */
public final class StageStrategySU2 implements LiveStrategySU2 {
    public static final String PREFIX = "SU2S|1|";
    public static final int MAX_CANDIDATES = 12, MAX_BUNDLE_BYTES = 262144;
    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();
    static final ModelGenomeSU2.Member REGIME = regime();
    private static ModelGenomeSU2.Member regime() {
        Map<String, String> p = new TreeMap<>();
        p.put("K", "3"); p.put("stay", "0.995"); p.put("lr", "0.01"); p.put("seed", "0"); p.put("temper", "0.3");
        return new ModelGenomeSU2.Member("REGMEM", p, 1);
    }

    public static boolean isStage(String text) { return text != null && text.startsWith(PREFIX); }
    public static String keyOf(String text) { return HashingSU1.text("SU2_STAGE_V1|" + text); }
    public static synchronized void register(Definition d) {
        DEFINITIONS.put(d.text(), d);
        while (DEFINITIONS.size() > 64) DEFINITIONS.remove(DEFINITIONS.keySet().iterator().next());
    }
    public static synchronized Definition definition(String text) {
        Definition d = DEFINITIONS.get(text);
        if (d == null) throw new IllegalStateException("SU2_STAGE_BUNDLE_UNAVAILABLE");
        return d;
    }
    static List<ModelGenomeSU2> programs(String text) {
        return CommitteeSU2.isCommittee(text) ? CommitteeSU2.parse(text).subs : Collections.singletonList(ModelGenomeSU2.parse(text));
    }
    public static final class Definition {
        public final String tier; public final double coverage; public final long through;
        public final int baseIndex, validationActs, validationHits, baseActs, baseHits;
        public final List<String> candidates; final StageLayerSU2.Layer layer; final double[] seedScores;
        public Definition(String tier, double coverage, long through, List<String> candidates, StageLayerSU2.Layer layer, double[] seedScores) {
            this(tier, coverage, through, candidates, layer, seedScores, 0, 0, 0, 0, 0);
        }
        public Definition(String tier, double coverage, long through, List<String> candidates, StageLayerSU2.Layer layer, double[] seedScores, int baseIndex, int validationActs, int validationHits, int baseActs, int baseHits) {
            if (candidates.isEmpty() || candidates.size() > MAX_CANDIDATES || seedScores.length > 720 || seedScores.length < 100 || coverage <= 0 || coverage > 1)
                throw new IllegalArgumentException("SU2_STAGE_SHAPE");
            this.tier = tier; this.coverage = coverage; this.through = through;
            this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates)); this.layer = layer; this.seedScores = seedScores.clone();
            this.baseIndex = baseIndex; this.validationActs = validationActs; this.validationHits = validationHits; this.baseActs = baseActs; this.baseHits = baseHits;
        }
        public byte[] bytes() {
            try {
                ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b);
                o.writeInt(1); o.writeUTF(tier); o.writeDouble(coverage); o.writeLong(through);
                o.writeInt(baseIndex); o.writeInt(validationActs); o.writeInt(validationHits); o.writeInt(baseActs); o.writeInt(baseHits); o.writeInt(candidates.size());
                for (String c : candidates) o.writeUTF(c);
                matrix(o, layer.gmm.mu); matrix(o, layer.gmm.var); vector(o, layer.gmm.w); o.writeDouble(layer.gmm.bic);
                vector(o, layer.mu); vector(o, layer.sd); matrix(o, layer.W); vector(o, layer.last); vector(o, seedScores); o.flush();
                if (b.size() > MAX_BUNDLE_BYTES) throw new IllegalArgumentException("SU2_STAGE_BUNDLE_BUDGET");
                return b.toByteArray();
            } catch (IOException impossible) { throw new IllegalStateException(impossible); }
        }
        public String text() { return PREFIX + HashingSU1.bytes(bytes()); }
        public String key() { return keyOf(text()); }
        public String formula() { return "阶段层·" + layer.stages() + "阶段·" + candidates.size() + "候选·前" + Math.round(coverage * 100) + "%择期（独立验证优于委员会后部署）"; }
        public Set<String> calibrators() {
            Set<String> out = new HashSet<>();
            for (String text : candidates) for (ModelGenomeSU2 g : programs(text)) if (g.calibrated) for (ModelGenomeSU2.Member m : g.members) out.add(m.modelKey());
            return out;
        }
        public static Definition read(byte[] bytes) throws IOException {
            if (bytes.length > MAX_BUNDLE_BYTES) throw new IOException("SU2_STAGE_BUNDLE_BUDGET");
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readInt() != 1) throw new IOException("SU2_STAGE_VERSION");
            String tier = in.readUTF(); double cov = in.readDouble(); long seq = in.readLong();
            int base = count(in, MAX_CANDIDATES), va = count(in, 100000), vh = count(in, va), ba = count(in, 100000), bh = count(in, ba);
            int n = count(in, MAX_CANDIDATES); List<String> cand = new ArrayList<>();
            for (int i = 0; i < n; i++) { String text = in.readUTF(); programs(text); cand.add(text); }
            StageLayerSU2.Gmm g = new StageLayerSU2.Gmm(matrix(in), matrix(in), vector(in), in.readDouble());
            StageLayerSU2.Layer l = new StageLayerSU2.Layer(g, vector(in), vector(in), matrix(in), vector(in));
            Definition d = new Definition(tier, cov, seq, cand, l, vector(in), base, va, vh, ba, bh);
            if (in.available() != 0 || base >= n || l.W.length != n || l.mu.length != StageLayerSU2.BASE_COLS + n) throw new IOException("SU2_STAGE_BUNDLE_SHAPE");
            return d;
        }
    }
    static int count(DataInput in, int cap) throws IOException { int n = in.readInt(); if (n < 0 || n > cap) throw new IOException("SU2_STAGE_ARRAY_BUDGET"); return n; }
    static void vector(DataOutput o, double[] v) throws IOException { o.writeInt(v.length); for (double x : v) o.writeDouble(x); }
    static double[] vector(DataInput in) throws IOException { double[] v = new double[count(in, 2048)]; for (int i = 0; i < v.length; i++) { v[i] = in.readDouble(); if (!Double.isFinite(v[i])) throw new IOException("SU2_STAGE_NONFINITE"); } return v; }
    static void matrix(DataOutput o, double[][] m) throws IOException { o.writeInt(m.length); for (double[] r : m) vector(o, r); }
    static double[][] matrix(DataInput in) throws IOException { double[][] m = new double[count(in, 128)][]; for (int i = 0; i < m.length; i++) m[i] = vector(in); return m; }

    final Definition def; final ModelLiveSU2.Hub hub; final LiveStrategySU2[] subs; final StageLayerSU2.Live filter;
    final int[][] history = new int[120][]; final boolean[][] hits; final double[] scores = new double[720];
    final long start; long t; int historyN; double threshold; ModelLiveSU2.Decision pending;
    ModelLiveSU2.Decision[] choices; float[][] pooled;

    public StageStrategySU2(Definition d, ModelLiveSU2.Hub hub, long genesis, int warmup, int window, int refit) {
        this.def = d; this.hub = hub; this.start = d.through - genesis + 1; this.filter = new StageLayerSU2.Live(d.layer);
        subs = new LiveStrategySU2[d.candidates.size()]; hits = new boolean[subs.length][50];
        for (int i = 0; i < subs.length; i++) {
            String text = d.candidates.get(i);
            subs[i] = CommitteeSU2.isCommittee(text) ? new CommitteeLiveSU2(CommitteeSU2.parse(text), hub, warmup, window, refit) : new ModelLiveSU2(ModelGenomeSU2.parse(text), hub, warmup, window, refit);
        }
        hub.model(REGIME);
        for (int i = 0; i < d.seedScores.length; i++) scores[Math.floorMod(start - d.seedScores.length + i, 720)] = d.seedScores[i];
    }
    public String key() { return def.key(); }
    public boolean hasPending() { return pending != null; }
    public float[][] pendingTable(long seq) { return pending != null && t == seq ? pooled : null; }
    public ModelLiveSU2.Decision decide() {
        if (pending != null) return pending;
        choices = new ModelLiveSU2.Decision[subs.length]; double[] conf = new double[subs.length], perf = new double[subs.length];
        for (int i = 0; i < subs.length; i++) { choices[i] = subs[i].decide(); conf[i] = choices[i].phat; int n = (int) Math.min(t, 50); int k = 0; for (int u = 0; u < n; u++) if (hits[i][u]) k++; perf[i] = n < 50 ? .3 : k / 50.0; }
        int chosen = 0; double score = conf[0]; boolean act = false;
        if (t >= start) {
            int[][] h = new int[historyN][]; for (int i = 0; i < historyN; i++) h[i] = history[Math.floorMod(t - historyN + i, 120)];
            hub.predict(); float[] st = hub.st.get(REGIME.modelKey());
            double[] p = filter.step(StageLayerSU2.row(h, h.length, st[0], perf), conf);
            for (int i = 1; i < p.length; i++) if (p[i] > p[chosen]) chosen = i;
            score = p[chosen];
            if ((t - start) % 25 == 0 || t == 0) {
                int n = (int) Math.min(720, def.seedScores.length + Math.max(0, t - start));
                double[] previous = new double[n]; for (int i = 0; i < n; i++) previous[i] = scores[Math.floorMod(t - n + i, 720)];
                threshold = ModelReplaySU2.quantileLinear(previous, 1 - def.coverage);
            }
            act = score >= threshold && conf[chosen] > ModelReplaySU2.INFORMATIVE;
        }
        ModelLiveSU2.Decision pick = choices[chosen]; pooled = subs[chosen].pendingTable(t);
        pending = new ModelLiveSU2.Decision(t, pick.position, pick.mask, act, score, score);
        return pending;
    }
    public boolean settle(int[] draw) {
        if (pending == null) throw new IllegalStateException("SU2_STAGE_SETTLE_WITHOUT_FREEZE");
        for (int i = 0; i < subs.length; i++) hits[i][(int) (t % 50)] = subs[i].settle(draw);
        boolean hit = (pending.mask & (1 << (draw[pending.position - 1] - 1))) != 0;
        if (t >= start) scores[(int) (t % 720)] = pending.gscore;
        history[(int) (t % 120)] = draw.clone(); historyN = Math.min(120, historyN + 1); t++; pending = null;
        return hit;
    }
    public void write(DataOutput o) throws IOException {
        o.writeUTF(key()); o.writeLong(t); o.writeInt(historyN); o.writeDouble(threshold); vector(o, scores); vector(o, filter.a);
        for (int i = 0; i < historyN; i++) for (int x : history[Math.floorMod(t - historyN + i, 120)]) o.writeInt(x);
        for (boolean[] row : hits) for (boolean x : row) o.writeBoolean(x);
        for (LiveStrategySU2 sub : subs) sub.write(o);
    }
    public void readInto(DataInput in) throws IOException {
        if (!key().equals(in.readUTF())) throw new IOException("SU2_STAGE_STATE_KEY");
        t = in.readLong(); historyN = count(in, 120); threshold = in.readDouble(); double[] s = vector(in), a = vector(in);
        if (s.length != 720 || a.length != def.layer.stages()) throw new IOException("SU2_STAGE_STATE_SHAPE");
        System.arraycopy(s, 0, scores, 0, 720); filter.a = a;
        for (int i = 0; i < historyN; i++) { int[] draw = new int[10]; for (int p = 0; p < 10; p++) draw[p] = in.readInt(); history[Math.floorMod(t - historyN + i, 120)] = draw; }
        for (boolean[] row : hits) for (int i = 0; i < row.length; i++) row[i] = in.readBoolean();
        for (LiveStrategySU2 sub : subs) sub.readInto(in); pending = null;
    }

    /** Fit on settled history, guard on a disjoint validation suffix, then refit the accepted layer on all history. */
    public static List<Definition> prepare(ModelEvolutionSU2.State state, ModelReplaySU2.Library lib, long through) {
        int[][] D = lib.draws(); int T = D.length, V = Math.min(1500, Math.max(700, T / 5));
        if (T - V < 1800 || state.committees.isEmpty()) return Collections.emptyList();
        List<String> texts = new ArrayList<>(); Map<String, Integer> tierIndex = new LinkedHashMap<>(); Set<String> cals = new HashSet<>();
        for (String tier : ModelEvolutionSU2.TIERS) { CommitteeSU2 c = state.committees.get(tier); if (c != null && add(texts, cals, c.text())) tierIndex.put(tier, texts.indexOf(c.text())); }
        for (ModelEvolutionSU2.Ranked r : state.hallOfFame.subList(0, Math.min(8, state.hallOfFame.size()))) add(texts, cals, r.g.text());
        ModelGenomeSU2 contmat = new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("CONTMAT", Collections.emptyMap(), 1)), "STATIC", false, "MASS", 1, .2, true);
        if (texts.size() == MAX_CANDIDATES) texts.remove(texts.size() - 1);
        add(texts, cals, contmat.text());
        int S = texts.size(); double[][] conf = new double[S][]; boolean[][] hit = new boolean[S][];
        for (int i = 0; i < S; i++) { String text = texts.get(i); ModelReplaySU2.Result r = CommitteeSU2.isCommittee(text) ? CommitteeSU2.evaluate(CommitteeSU2.parse(text), lib, 1500) : ModelReplaySU2.evaluate(ModelGenomeSU2.parse(text), lib, 1500, 2500, 250); conf[i] = r.phat; hit[i] = r.hit; }
        float[][] rs = lib.get(REGIME).state; double[] st0 = new double[T]; for (int i = 0; i < T; i++) st0[i] = rs[i][0];
        double[][] F = StageLayerSU2.fingerprints(D, hit, st0); int cut = T - V;
        StageLayerSU2.Layer val = StageLayerSU2.fit(F, conf, hit, 1500, cut);
        double[][] pv = StageLayerSU2.apply(val, F, conf, cut, T), warm = StageLayerSU2.apply(val, F, conf, cut - 720, cut);
        StageLayerSU2.Layer full = StageLayerSU2.fit(F, conf, hit, 1500, T);
        double[][] seed = StageLayerSU2.apply(full, F, conf, T - 720, T); double[] initial = maxima(seed);
        List<Definition> out = new ArrayList<>(); double[] max = maxima(pv), hist = maxima(warm);
        for (Map.Entry<String, Integer> e : tierIndex.entrySet()) {
            double cov = e.getKey().equals("F15") ? .15 : e.getKey().equals("F30") ? .30 : .50;
            int acts = 0, wins = 0, baseActs = 0, baseWins = 0; double threshold = 1, baseThreshold = 1;
            for (int u = 0; u < V; u++) {
                if (u % 25 == 0) { double[] prior = new double[720], priorBase = new double[720]; for (int j = 0; j < 720; j++) { int v = u - 720 + j; prior[j] = v < 0 ? hist[hist.length + v] : max[v]; priorBase[j] = conf[e.getValue()][cut + v]; } threshold = ModelReplaySU2.quantileLinear(prior, 1 - cov); baseThreshold = ModelReplaySU2.quantileLinear(priorBase, 1 - cov); }
                int best = 0; for (int i = 1; i < S; i++) if (pv[u][i] > pv[u][best]) best = i;
                if (max[u] >= threshold && conf[best][cut + u] > ModelReplaySU2.INFORMATIVE) { acts++; if (hit[best][cut + u]) wins++; }
                if (conf[e.getValue()][cut + u] >= baseThreshold && conf[e.getValue()][cut + u] > ModelReplaySU2.INFORMATIVE) { baseActs++; if (hit[e.getValue()][cut + u]) baseWins++; }
            }
            if (acts >= 30 && baseActs > 0 && wins / (double) acts > baseWins / (double) baseActs) out.add(new Definition(e.getKey(), cov, through, texts, full, initial, e.getValue(), acts, wins, baseActs, baseWins));
        }
        return out;
    }
    static double[] maxima(double[][] p) { double[] out = new double[p.length]; for (int i = 0; i < p.length; i++) { out[i] = p[i][0]; for (int j = 1; j < p[i].length; j++) out[i] = Math.max(out[i], p[i][j]); } return out; }
    static boolean add(List<String> texts, Set<String> cals, String text) {
        if (texts.contains(text)) return true;
        if (texts.size() >= MAX_CANDIDATES) return false;
        Set<String> union = new HashSet<>(cals); for (ModelGenomeSU2 g : programs(text)) if (g.calibrated) for (ModelGenomeSU2.Member m : g.members) union.add(m.modelKey());
        if (union.size() > 32) return false;
        cals.clear(); cals.addAll(union); texts.add(text); return true;
    }
}
