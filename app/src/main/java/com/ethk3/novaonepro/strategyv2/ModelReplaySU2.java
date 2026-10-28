package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * Complete R13 strategy replay (P18/P19): members -> pool -> optional Sinkhorn ->
 * best position Top3 -> gate (MASS or the META applicability learner) -> block-causal
 * frequency threshold. Every period is computed and settled (hit is defined for
 * every t), which is what the applicability learner trains on.
 *
 * Mirrors foundry13.evaluate() in the Python reference.
 */
public final class ModelReplaySU2 {
    private ModelReplaySU2() {}

    public static final double INFORMATIVE = 0.3 + 1e-6;

    public static final class Result {
        public final boolean[] act, hit; public final int[] position, mask; public final double[] gscore, phat;
        Result(int n) { act = new boolean[n]; hit = new boolean[n]; position = new int[n]; mask = new int[n]; gscore = new double[n]; phat = new double[n]; Arrays.fill(phat, Double.NaN); }
        public int actions(int a, int b) { int n = 0; for (int t = a; t < b; t++) if (act[t]) n++; return n; }
        public int hits(int a, int b) { int n = 0; for (int t = a; t < b; t++) if (act[t] && hit[t]) n++; return n; }
    }

    /** Caches every (kind, parameters) model once per dataset; bounded by bytes (P30 budget). */
    public static final class Library {
        final int[][] draws; final long byteCap; long bytes;
        final String snapshotBinding;final long sourceThrough;
        public interface DurableOutputs {OnlineModelsSU2.Output raw(ModelGenomeSU2.Member member,int[][] draws);OnlineModelsSU2.Output calibrated(ModelGenomeSU2.Member member,int[][] draws,OnlineModelsSU2.Output raw);OnlineModelsSU2.Model trained(ModelGenomeSU2.Member member,int through);CalibrationSU2.Live calibrator(ModelGenomeSU2.Member member,int through);}
        final DurableOutputs durable;
        public ModelGenomeSU2 bind(ModelGenomeSU2 g){if(WarmModelSeedR26.has(g))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED_FOR_REPLAY");return snapshotBinding==null?g:g.bindBirth(snapshotBinding,sourceThrough);}
        final LinkedHashMap<String, OnlineModelsSU2.Output> cache = new LinkedHashMap<>(16, .75f, true);
        public Library(int[][] draws,long byteCap){this(draws,byteCap,null,0,null);}
        public Library(int[][] draws,long byteCap,String snapshot,long through,DurableOutputs durable){OnlineModelsSU2.requireDraws(draws);this.draws=draws;this.byteCap=byteCap;snapshotBinding=snapshot;sourceThrough=through;this.durable=durable;}
        public OnlineModelsSU2.Output get(ModelGenomeSU2.Member m) {
            if(WarmModelSeedR26.has(m.params))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
            String key = m.modelKey();
            OnlineModelsSU2.Output o = cache.get(key);
            if (o != null) return o;
            o = durable==null?compute(draws,m.kind,m.params):durable.raw(m,draws);
            cache.put(key, o); bytes += o.bytes();
            Iterator<Map.Entry<String, OnlineModelsSU2.Output>> it = cache.entrySet().iterator();
            while (bytes > byteCap && cache.size() > 1 && it.hasNext()) { Map.Entry<String, OnlineModelsSU2.Output> e = it.next(); if (e.getKey().equals(key)) continue; bytes -= e.getValue().bytes(); it.remove(); }
            return o;
        }
        final Map<Integer, float[][]> stateSeries = new HashMap<>();
        /** R15 (P45): indicator series of registry state atom j over this history (teacher matrix pool). */
        public float[][] states(int j) { return stateSeries.computeIfAbsent(j, k -> TeacherPoolSU2.states(draws, k)); }
        /** R14 (P36): the member's table temperature-calibrated online (CalibrationSU2), cached separately. */
        public OnlineModelsSU2.Output getCal(ModelGenomeSU2.Member m) {
            String key = m.modelKey() + "|CAL";
            OnlineModelsSU2.Output o = cache.get(key);
            if (o != null) return o;
            OnlineModelsSU2.Output raw = get(m);
            o = durable==null?new OnlineModelsSU2.Output(CalibrationSU2.calibrate(raw.p,draws),raw.state):durable.calibrated(m,draws,raw);
            cache.put(key, o); bytes += (long) o.p.length * 400L;
            Iterator<Map.Entry<String, OnlineModelsSU2.Output>> it = cache.entrySet().iterator();
            while (bytes > byteCap && cache.size() > 2 && it.hasNext()) { Map.Entry<String, OnlineModelsSU2.Output> e = it.next(); if (e.getKey().equals(key) || e.getKey().equals(m.modelKey())) continue; bytes -= e.getValue().bytes(); it.remove(); }
            return o;
        }
        public int[][] draws() { return draws; }
    }

    static double d(Map<String, String> p, String k) { String v = p.get(k); if (v == null) throw new IllegalArgumentException("SU2_PARAM_MISSING:" + k); return Double.parseDouble(v); }
    static double[] tuple(String s) {
        String t = s.replace("(", "").replace(")", "").trim();
        if (t.endsWith(",")) t = t.substring(0, t.length() - 1);
        String[] f = t.split(",");
        double[] out = new double[f.length];
        for (int i = 0; i < f.length; i++) out[i] = Double.parseDouble(f[i].trim());
        return out;
    }
    /** "()" / "(1,)" / "(1, 2)" -> int[] (empty allowed). */
    static int[] ints(String s) {
        String t = s.replace("(", "").replace(")", "").replace(" ", "");
        if (t.endsWith(",")) t = t.substring(0, t.length() - 1);
        if (t.isEmpty()) return new int[0];
        String[] f = t.split(","); int[] out = new int[f.length];
        for (int i = 0; i < f.length; i++) out[i] = Integer.parseInt(f[i]);
        return out;
    }
    static int[][] pairsOrNone(String s) { return s.replace("(", "").replace(")", "").replace(",", "").trim().isEmpty() ? new int[0][] : pairs(s); }
    static int[][] pairs(String s) {
        double[] flat = tuple(s);
        if (flat.length % 2 != 0) throw new IllegalArgumentException("SU2_SOURCES");
        int[][] out = new int[flat.length / 2][2];
        for (int i = 0; i < out.length; i++) { out[i][0] = (int) flat[2 * i]; out[i][1] = (int) flat[2 * i + 1]; }
        return out;
    }

    public interface Adapter { OnlineModelsSU2.Model build(Map<String, String> params); }
    /** Adapter kinds registered by the app at start (ProgramModelSU2.register()); the core stays pure Java. */
    public static final Map<String, Adapter> ADAPTERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Builds the stepwise model named by a member (same constructor the live loop uses). */
    public static OnlineModelsSU2.Model build(String kind, Map<String, String> p) {
        if(WarmModelSeedR26.has(p))return build(kind,WarmModelSeedR26.structuralParameters(p));
        if(p.containsKey("_birth_snapshot")||p.containsKey("_birth_through")){Map<String,String> raw=new TreeMap<>(p);String birth=raw.remove("_birth_snapshot"),through=raw.remove("_birth_through");if(birth==null||!birth.matches("[0-9a-f]{64}")||through==null||!through.matches("[0-9]+"))throw new IllegalArgumentException("R26_BIRTH_BINDING");return build(kind,raw);}

        if(p.containsKey("_legal_head")){if(!"PERMUTATION_V1".equals(p.get("_legal_head")))throw new IllegalArgumentException("R26_LEGAL_HEAD");Map<String,String> raw=new TreeMap<>(p);raw.remove("_legal_head");return build(kind,raw);}
        Adapter adapter = ADAPTERS.get(kind);
        if (adapter != null) return adapter.build(p);
        if(kind.equals("R26SCALE")){if(!"1".equals(p.get("abi"))||p.size()!=2||!("SIX".equals(p.get("mode"))||"SHORT".equals(p.get("mode"))))throw new IllegalArgumentException("R26_SCALE_PARAMETERS");return new ScaleEncoderR26("SIX".equals(p.get("mode")));}
        if(kind.equals("R26F01")&&p.containsKey("lags")){if(!"1".equals(p.get("abi"))||p.size()!=3||!p.get("lags").matches("[1-8]")||!Arrays.asList("true","false").contains(p.get("cross")))throw new IllegalArgumentException("F01_GRAPH_PARAMETERS");return new FamilyF01KernelR26(Integer.parseInt(p.get("lags")),Boolean.parseBoolean(p.get("cross")));}
        if(kind.equals("R26F02")&&p.containsKey("graph")){if(!"1".equals(p.get("abi"))||p.size()!=3||!p.get("graph").matches("[0-2]")||!Arrays.asList("true","false").contains(p.get("factors")))throw new IllegalArgumentException("F02_GRAPH_PARAMETERS");return new FamilyF02KernelR26(Integer.parseInt(p.get("graph")),Boolean.parseBoolean(p.get("factors")));}
        if(kind.equals("R26F08")&&p.containsKey("K")){if(!"1".equals(p.get("abi"))||p.size()!=3||!String.valueOf(p.get("K")).matches("[2-6]")||!Arrays.asList("true","false").contains(p.get("transition")))throw new IllegalArgumentException("F08_GRAPH_PARAMETERS");return new FamilyF08KernelR26(Integer.parseInt(p.get("K")),Boolean.parseBoolean(p.get("transition")));}
        if(kind.equals("R26F19")){
            if(p.equals(Collections.singletonMap("abi","1")))return new FamilyF19KernelR26(.02);
            if(p.size()!=6||!"1".equals(p.get("abi"))||!"2".equals(p.get("tensor"))||!"ADAGRAD_V1".equals(p.get("optimizer"))||!Arrays.asList("true","false").contains(p.get("position"))||!Arrays.asList("NONE","FIRST_POSITION_HALF").contains(p.get("state"))||!Arrays.asList("NONE","NO_POSITION","NO_STATE").contains(p.get("control")))throw new IllegalArgumentException("F19_TENSOR_PARAMETERS");
            boolean position=Boolean.parseBoolean(p.get("position")),state=!"NONE".equals(p.get("state"));String control=p.get("control");
            if(!position&&state||control.equals("NONE")&&!position||control.equals("NO_POSITION")&&(position||state)||control.equals("NO_STATE")&&(!position||state))throw new IllegalArgumentException("F19_TENSOR_CONTROL_GRAPH");
            return new FamilyF19TensorR26(.02,position,state);
        }
        if(kind.equals("R26F20")){
            if(p.equals(Collections.singletonMap("abi","1")))return build("REGMEM",ModelEvolutionSU2.centralParams("REGMEM"));
            if(!"1".equals(p.get("abi"))||!"1".equals(p.get("fragments"))||p.size()!=4||!Arrays.asList("8","16","24","32").contains(p.get("slots"))||!Arrays.asList("MATCH","NONE","MISMATCH","EXPIRED").contains(p.get("retrieval")))throw new IllegalArgumentException("F20_GRAPH_PARAMETERS");
            return new FamilyF20KernelR26(Integer.parseInt(p.get("slots")),FamilyF20KernelR26.Retrieval.valueOf(p.get("retrieval")));
        }
        if(kind.matches("R26F0[3-7]")&&p.containsKey("graph")){if(!"1".equals(p.get("abi"))||p.size()!=2||!p.get("graph").matches("[0-2]"))throw new IllegalArgumentException("R26_NEURAL_GRAPH_PARAMETERS");return new NeuralFamilyR26(kind.substring(3),Integer.parseInt(p.get("graph")));}
        if(kind.startsWith("R26")){if(!"1".equals(p.get("abi"))||p.size()!=1)throw new IllegalArgumentException("R26_FAMILY_ABI");return FamilyRegistryR26.build(kind.substring(3));}
        switch (kind) {
            case "MATCHING": return new PermutationModelR26(d(p,"lr"));
            case "MSDIR": return new OnlineModelsSU2.MsDir(tuple(p.get("scales")), d(p, "alpha"), d(p, "share"));
            case "BOCPD": return new OnlineModelsSU2.Bocpd(d(p, "hazard"), d(p, "alpha"), (int) d(p, "R"));
            case "HMM": return new OnlineModelsSU2.Hmm((int) d(p, "K"), d(p, "stay"), d(p, "lr"), (long) d(p, "seed"), d(p, "temper"));
            case "LOGLIN": return new OnlineModelsSU2.LogLin(pairs(p.get("sources")), d(p, "lr"), d(p, "l2"));
            case "ATOMNET": return AtomNetSU2.of(p);
            case "CONTMAT": return new ContMatSU2();
            case "REGMEM": return new OnlineModelsSU2.RegMem((int) d(p, "K"), d(p, "stay"), d(p, "lr"), (long) d(p, "seed"), d(p, "temper"), 0.05);
            case "RELMEM": return new OnlineModelsSU2.RelMem((int) d(p, "K"), ints(p.get("lags")), pairsOrNone(p.get("sources")), d(p, "stay"), d(p, "lr"), d(p, "l2"), 0.999, d(p, "temper"), (long) d(p, "seed"), 0.05);
            case "KALMAN": return new OnlineModelsSU2.Kalman(d(p, "q_level"), d(p, "q_slope"), 0.09, d(p, "horizon"), d(p, "temp"));
            case "RELBAYES": {
                double[] l = tuple(p.get("lags")); int[] lags = new int[l.length]; for (int i = 0; i < l.length; i++) lags[i] = (int) l[i];
                return new OnlineModelsSU2.RelBayes(lags, d(p, "decay"), d(p, "kappa"), true, 1.0);
            }
            default: throw new IllegalArgumentException("SU2_KIND");
        }
    }

    public static OnlineModelsSU2.Output compute(int[][] draws, String kind, Map<String, String> p) {
        if(WarmModelSeedR26.has(p))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
        return OnlineModelsSU2.run(build(kind, p), draws);
    }

    /** Member tables (calibrated when the genome says so) -> pool -> optional Sinkhorn. R14: STATIC weights normalised. */
    public static float[][][] pooledTable(ModelGenomeSU2 g, Library lib) {
        g=lib.bind(g);
        int M = g.members.size();
        float[][][][] Ps = new float[M][][][]; float[][][] STs = new float[M][][]; double[] w = new double[M]; double ws = 0;
        for (int m = 0; m < M; m++) {ExecutionBudgetR26.poll(); OnlineModelsSU2.Output o = g.calibrated ? lib.getCal(g.members.get(m)) : lib.get(g.members.get(m)); Ps[m] = o.p; STs[m] = o.state; w[m] = g.members.get(m).weight; ws += w[m]; }
        if (g.calibrated) for (int m = 0; m < M; m++) w[m] /= ws;
        float[][][] P = M == 1 ? Ps[0] : TeacherPoolSU2.isTeacher(g.pool) ? TeacherPoolSU2.pool(Ps, lib.draws, lib.states(TeacherPoolSU2.stateOf(g.pool)))
            : g.pool.equals("BMA") ? ModelOpsSU2.poolBma(Ps, lib.draws, 0.02, 0.995)
            : g.pool.equals("GATED") ? ModelOpsSU2.poolGated(Ps, STs, lib.draws, 0.03, 1e-3) : ModelOpsSU2.poolStatic(Ps, w);
        if(g.legalReadoutR26()){float[][][] legal=new float[P.length][][];for(int t=0;t<P.length;t++){ExecutionBudgetR26.poll();legal[t]=LegalReadoutR26.project(P[t]);}P=legal;}
        else if (g.sink) P = ModelOpsSU2.sink(P, 20);
        return P;
    }

    /** A probability table used as a strategy with the confidence gate at `coverage` (block-causal threshold). */
    public static Result evaluateTable(float[][][] P, int[][] draws, double coverage, int startMeta) {
        int T = draws.length; Result r = new Result(T); Integer[] ord = new Integer[10];
        for (int t = 0; t < T; t++) {ExecutionBudgetR26.poll();
            double[] mass = new double[10]; int[][] top = new int[10][3];
            for (int p = 0; p < 10; p++) {
                final float[] row = P[t][p];
                for (int i = 0; i < 10; i++) ord[i] = i;
                Arrays.sort(ord, (a, b) -> Double.compare((double) row[b] - b * 1e-12, (double) row[a] - a * 1e-12));
                double s = 0; for (int i = 0; i < 3; i++) { top[p][i] = ord[i]; s += (double) row[ord[i]]; }
                mass[p] = Math.rint(s * 1e9) / 1e9;
            }
            int p1 = 0; for (int p = 1; p < 10; p++) if (mass[p] > mass[p1]) p1 = p;
            r.position[t] = p1 + 1; r.mask[t] = (1 << top[p1][0]) | (1 << top[p1][1]) | (1 << top[p1][2]);
            int x = draws[t][p1] - 1; r.hit[t] = top[p1][0] == x || top[p1][1] == x || top[p1][2] == x;
            r.gscore[t] = mass[p1]; r.phat[t] = mass[p1];
        }
        // R14: a calibrated table at (or below) chance has nothing to say - never act on it
        if (coverage >= .999) { for (int t = startMeta; t < T; t++) r.act[t] = r.gscore[t] > INFORMATIVE; }
        else for (int b = startMeta; b < T; b += 50) {ExecutionBudgetR26.poll();
            int lo = Math.max(startMeta - 720, b - 720); int n = b - lo;
            if (n < 100) continue;
            double thr = quantileLinear(Arrays.copyOfRange(r.gscore, lo, b), 1 - coverage);
            for (int t = b; t < Math.min(b + 50, T); t++) r.act[t] = r.gscore[t] >= thr && r.gscore[t] > INFORMATIVE;
        }
        return r;
    }

    public static Result evaluate(ModelGenomeSU2 g, Library lib, int startMeta, int window, int refit) {
        g=lib.bind(g);
        startMeta=g.gateWarmup(startMeta);
        if(g.members.size()==1&&(g.members.get(0).kind.equals("R26F15")||g.members.get(0).kind.equals("R26F16"))){
            int[][] draws=lib.draws;Result result=new Result(draws.length);ModelLiveSU2.Hub hub=new ModelLiveSU2.Hub();ModelLiveSU2 live=new ModelLiveSU2(g,hub,startMeta,window,refit);
            for(int t=0;t<draws.length;t++){ExecutionBudgetR26.poll();ModelLiveSU2.Decision d=live.decide();result.act[t]=d.act;result.position[t]=d.position;result.mask[t]=d.mask;result.gscore[t]=d.gscore;result.phat[t]=d.phat;result.hit[t]=live.settle(draws[t]);hub.update(draws[t]);}return result;
        }

        int T = lib.draws.length;
        int M = g.members.size();
        float[][][][] Ps = new float[M][][][];
        float[][][] STs = new float[M][][];
        for (int m = 0; m < M; m++) {ExecutionBudgetR26.poll(); OnlineModelsSU2.Output o = g.calibrated ? lib.getCal(g.members.get(m)) : lib.get(g.members.get(m)); Ps[m] = o.p; STs[m] = o.state; }
        float[][][] P = pooledTable(g, lib);
        Result r = new Result(T);
        int[][] k3 = new int[T][3]; double[] conf = new double[T], margin = new double[T], ent = new double[T];
        Integer[] ord = new Integer[10];
        for (int t = 0; t < T; t++) {ExecutionBudgetR26.poll();
            double[] mass = new double[10]; int[][] top = new int[10][3];
            for (int p = 0; p < 10; p++) {
                final float[] row = P[t][p];
                for (int i = 0; i < 10; i++) ord[i] = i;
                Arrays.sort(ord, (a, b) -> Double.compare((double) row[b] - b * 1e-12, (double) row[a] - a * 1e-12));
                double s = 0; for (int i = 0; i < 3; i++) { top[p][i] = ord[i]; s += (double) row[ord[i]]; }
                mass[p] = Math.rint(s * 1e9) / 1e9;
            }
            int p1 = 0; for (int p = 1; p < 10; p++) if (mass[p] > mass[p1]) p1 = p;
            int p2 = p1 == 0 ? 1 : 0; for (int p = 0; p < 10; p++) if (p != p1 && mass[p] > mass[p2]) p2 = p;
            k3[t] = top[p1]; r.position[t] = p1 + 1;
            r.mask[t] = (1 << top[p1][0]) | (1 << top[p1][1]) | (1 << top[p1][2]);
            int x = lib.draws[t][p1] - 1;
            r.hit[t] = top[p1][0] == x || top[p1][1] == x || top[p1][2] == x;
            conf[t] = mass[p1]; margin[t] = mass[p1] - mass[p2];
            double e = 0; for (int n = 0; n < 10; n++) { double v = P[t][p1][n]; e -= v * Math.log(v + 1e-12); } ent[t] = e / Math.log(10);
        }
        if (g.gate.equals("META")) {
            List<double[]> cols = new ArrayList<>();
            cols.add(conf); cols.add(margin); cols.add(ent); cols.add(rollingZ(conf, 720));
            for (int k : new int[]{5, 7}) {
                double[] mean = new double[T], min = new double[T];
                for (int t = 0; t < T; t++) {ExecutionBudgetR26.poll();
                    double s = 0, mn = 1e9; int p1 = r.position[t] - 1;
                    for (int m = 0; m < M; m++) {ExecutionBudgetR26.poll();
                        final float[] row = Ps[m][t][p1];
                        for (int i = 0; i < 10; i++) ord[i] = i;
                        Arrays.sort(ord, (a, b) -> Float.compare(row[b], row[a]));
                        int in = 0; for (int j = 0; j < 3; j++) for (int i = 0; i < k; i++) if (ord[i] == k3[t][j]) { in++; break; }
                        double sup = in / 3.0; s += sup; mn = Math.min(mn, sup);
                    }
                    mean[t] = s / M; min[t] = mn;
                }
                cols.add(mean); cols.add(min);
            }
            for (int m = 0; m < M; m++) for (int j = 0; j < STs[m][0].length; j++) { double[] c = new double[T]; for (int t = 0; t < T; t++) c[t] = STs[m][t][j]; cols.add(c); }
            for (int half : new int[]{12, 48, 200}) cols.add(emaHits(r.hit, half));
            int F = cols.size();
            double[][] X = new double[T][F];
            for (int j = 0; j < F; j++) { double[] c = cols.get(j); for (int t = 0; t < T; t++) X[t][j] = Double.isFinite(c[t]) ? c[t] : 0.0; }
            double[] wt = null, mu = new double[F], sd = new double[F]; int last = Integer.MIN_VALUE / 2;
            for (int t = startMeta; t < T; t++) {ExecutionBudgetR26.poll();
                if (wt == null || t - last >= refit) {
                    int lo = Math.max(0, t - window), n = t - lo;
                    if (n >= 400) {
                        for (int j = 0; j < F; j++) { double s = 0; for (int i = lo; i < t; i++) s += X[i][j]; mu[j] = s / n; double v = 0; for (int i = lo; i < t; i++) { double q = X[i][j] - mu[j]; v += q * q; } sd[j] = Math.sqrt(v / n) + 1e-9; }
                        double[][] Z = new double[n][F]; double[] y = new double[n];
                        for (int i = 0; i < n; i++) { for (int j = 0; j < F; j++) Z[i][j] = (X[lo + i][j] - mu[j]) / sd[j]; y[i] = r.hit[lo + i] ? 1 : 0; }
                        wt = LogisticRidgeSU2.fit(Z, y, n, g.metaC, 12); last = t;
                    }
                }
                if (wt != null) {
                    double z = wt[0]; for (int j = 0; j < F; j++) z += (X[t][j] - mu[j]) / sd[j] * wt[j + 1];
                    z = Math.max(-30, Math.min(30, z));
                    r.phat[t] = 1.0 / (1.0 + Math.exp(-z));
                }
            }
            for (int t = 0; t < T; t++) r.gscore[t] = Double.isNaN(r.phat[t]) ? -1.0 : r.phat[t];
        } else {
            for (int t = 0; t < T; t++) {ExecutionBudgetR26.poll(); r.gscore[t] = conf[t]; r.phat[t] = conf[t]; }
        }
        final boolean chanceGuard = g.calibrated && g.gate.equals("MASS");     // R14: never act on a chance-level table
        if (g.coverage >= .999) { for (int t = startMeta; t < T; t++) r.act[t] = !chanceGuard || r.gscore[t] > INFORMATIVE; }
        else for (int b = startMeta; b < T; b += 50) {ExecutionBudgetR26.poll();
            int lo = Math.max(g.gate.equals("META") ? startMeta : startMeta - 720, b - 720);
            double[] hist = new double[b - lo]; int n = 0;
            for (int t = lo; t < b; t++) if (r.gscore[t] > -1) hist[n++] = r.gscore[t];
            if (n < 100) continue;
            double thr = quantileLinear(Arrays.copyOf(hist, n), 1 - g.coverage);
            for (int t = b; t < Math.min(b + 50, T); t++) r.act[t] = r.gscore[t] >= thr && r.gscore[t] > -1 && (!chanceGuard || r.gscore[t] > INFORMATIVE);
        }
        if(g.legalReadoutR26())System.arraycopy(conf,0,r.phat,0,T);
        return r;
    }

    static double quantileLinear(double[] x, double q) {
        Arrays.sort(x); double pos = (x.length - 1) * q; int i = (int) Math.floor(pos);
        return i + 1 < x.length ? x[i] + (pos - i) * (x[i + 1] - x[i]) : x[i];
    }

    static double[] emaHits(boolean[] hit, int half) {
        double a = 1.0 - Math.pow(0.5, 1.0 / half), v = 0.3; double[] out = new double[hit.length];
        for (int t = 0; t < hit.length; t++) {ExecutionBudgetR26.poll(); out[t] = v; v = (1 - a) * v + a * (hit[t] ? 1.0 : 0.0); }
        return out;
    }

    static double[] rollingZ(double[] v, int w) {
        int T = v.length; double[] c1 = new double[T + 1], c2 = new double[T + 1], z = new double[T];
        for (int t = 0; t < T; t++) {ExecutionBudgetR26.poll(); c1[t + 1] = c1[t] + v[t]; c2[t + 1] = c2[t] + v[t] * v[t]; }
        for (int t = 0; t < T; t++) {ExecutionBudgetR26.poll();
            int lo = Math.max(t - w, 0), n = Math.max(t - lo, 1);
            double mu = (c1[t] - c1[lo]) / n, var = Math.max((c2[t] - c2[lo]) / n - mu * mu, 1e-12);
            double s = (v[t] - mu) / Math.sqrt(var);
            z[t] = t < 30 ? 0 : Math.max(-6, Math.min(6, s));
        }
        return z;
    }

    /** Frequency-aware stability fitness (P26): shrunk fold rates, mean - 0.25 spread, frequency floor. */
    public static double fitness(Result r, int a, int b, int folds, double minCov, double kappa) {
        double[] v = new double[folds]; int nAll = 0;
        for (int f = 0; f < folds; f++) {
            int x = a + (int) Math.floor((b - a) * (double) f / folds), y = a + (int) Math.floor((b - a) * (double) (f + 1) / folds);
            int n = r.actions(x, y), k = r.hits(x, y); nAll += n;
            v[f] = (k + 0.3 * kappa) / (n + kappa);
        }
        double mean = 0, mx = -1, mn = 2; for (double q : v) { mean += q; mx = Math.max(mx, q); mn = Math.min(mn, q); }
        mean /= folds;
        double cov = nAll / (double) (b - a), pen = cov >= minCov ? 0 : (minCov - cov) * 2;
        return mean - 0.25 * (mx - mn) - pen;
    }
}
