package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R12 reference: GRAMMAR program trees for production (U07). Parses the canonical text
 * produced by reference/replay/grammar.py and evaluates one target period from history
 * rows strictly before it. Semantics match the Python reference node for node
 * (ProgramTreeSU1Test checks parity on real draws).
 *
 * Built-in atoms are implemented here. Lifted registry atoms (ATOM/ATOMC) must be
 * evaluated in production by calling r952.AtomEvaluatorCP4 for every (position, digit)
 * and z-normalising across the ten numbers; the dataset-owned AtomLiftSU1 supplies them without global caches.
 *
 * Score tensors are double[10 positions][10 numbers]; each node output is rounded to
 * float32 exactly like the reference (numpy astype(float32)).
 */
public final class ProgramTreeSU1 {
    public static final String PROTOCOL = "SU1_GRAMMAR_V1";
    private static final int[] WINDOWS = {12, 24, 48, 96, 360};
    public static final class Fitted {
        public final RelationalStateInference.Weights weights;public final int firstTarget;
        public Fitted(RelationalStateInference.Weights weights,int firstTarget){this.weights=weights;this.firstTarget=firstTarget;}
    }

    /** History context for one target. draws[u][p] is the number (1..10) at position p of row u. */
    public static final class Context {
        final int[][] draws; final int t;
        final AtomLiftSU1 atoms; final ScoreHistoryCacheSU1 cache; final BudgetTokenSU1 budget;
        java.util.LinkedHashMap<String,RelationalStateInference.Weights> models=new java.util.LinkedHashMap<>(8,.75f,true);
        java.util.LinkedHashMap<String,FusionKernelSU1.Model> fusions=new java.util.LinkedHashMap<>(8,.75f,true);
        private Map<String,Fitted> fitted=java.util.Collections.emptyMap();
        private final Map<Integer, double[][]> experts = new HashMap<>();
        private final Map<Integer, double[][]> xtrans = new HashMap<>();
        public Context(int[][] draws, int target) {
            this(draws,target,null,new ScoreHistoryCacheSU1(768),new BudgetTokenSU1(60000,1000000,null));
        }
        public Context(int[][] draws,int target,AtomLiftSU1 atoms,ScoreHistoryCacheSU1 cache,BudgetTokenSU1 budget) {
            if(draws==null||target<0||target>draws.length)throw new IllegalArgumentException("TARGET");
            this.draws=draws;this.t=target;this.atoms=atoms;this.cache=cache;this.budget=budget;
        }
        public Context withFitted(Map<String,Fitted> bindings){fitted=bindings;return this;}
        Context atTarget(int target){Context c=new Context(draws,target,atoms,cache,budget);c.models=models;c.fusions=fusions;c.fitted=fitted;return c;}
        double[][] cached(String text,java.util.function.Supplier<double[][]> fn){budget.checkpointOrYield();return cache.score(text,t,fn);}
        int lagValue(int q, int lag) { return t >= lag ? draws[t - lag][q] : 0; }
        double[][] expert(int e) {
            double[][] v = experts.get(e);
            if (v == null) { v = cache.score("EXPERT("+e+")",t,()->Experts.z(draws,t,e)); experts.put(e, v); }
            return v;
        }
        double[][] xt(int off) {
            double[][] v = xtrans.get(off);
            if (v == null) { v = cache.transition(draws,t,off,budget); xtrans.put(off, v); }
            return v;
        }
    }

    public interface Node { double[][] score(Context c); }
    /** Conditions are evaluated per position; global conditions ignore p. */
    public interface Cond { boolean test(Context c, int p); }

    // ------------------------------------------------------------------ parsing
    public static Node parse(String text) {
        Parser p = new Parser(ProgramTextSU1.canonical(text));
        Node n = p.node();
        if (p.i != p.s.length()) throw new IllegalArgumentException("TRAILING_TEXT at " + p.i);
        return n;
    }

    private static final class Term {
        final String op; final List<Object> args;
        Term(String op, List<Object> args) { this.op = op; this.args = args; }
        int i(int k) { return Integer.parseInt((String) args.get(k)); }
        String s(int k) { return (String) args.get(k); }
        Term t(int k) { return (Term) args.get(k); }
        public String toString(){StringBuilder s=new StringBuilder(op).append('(');for(int i=0;i<args.size();i++){if(i>0)s.append(',');s.append(args.get(i));}return s.append(')').toString();}
    }

    private static final class Parser {
        final String s; int i;
        Parser(String s) { this.s = s; }
        char peek() { if (i >= s.length()) throw new IllegalArgumentException("UNEXPECTED_END"); return s.charAt(i); }
        Object item() {
            int start = i;
            while (i < s.length() && s.charAt(i) != '(' && s.charAt(i) != ',' && s.charAt(i) != ')') i++;
            String head = s.substring(start, i);
            if (i < s.length() && s.charAt(i) == '(') {
                i++;
                List<Object> args = new ArrayList<>();
                if (peek() != ')') {
                    while (true) {
                        args.add(item());
                        if (peek() == ',') { i++; continue; }
                        break;
                    }
                }
                if (peek() != ')') throw new IllegalArgumentException("EXPECTED_CLOSE at " + i);
                i++;
                return new Term(head, args);
            }
            if (head.isEmpty()) throw new IllegalArgumentException("EMPTY_TOKEN at " + i);
            return head;
        }
        Node node() { Object o = item(); if (!(o instanceof Term)) throw new IllegalArgumentException("NODE"); return build((Term) o); }
    }

    private static Node build(Term x) {
        final Node n=buildUncached(x);final String canonical=x.toString();
        return c->c.cached(canonical,()->n.score(c));
    }
    private static Node buildUncached(Term x) {
        switch (x.op) {
            case "EXPERT": { final int e = x.i(0); if (e < 0 || e > 7) throw new IllegalArgumentException("EXPERT");
                return c -> c.expert(e); }
            case "REL": { final String kind = x.s(0); final int lag = x.i(1), off = x.i(2), off2 = x.i(3);
                if (lag < 1 || off < 0 || off > 9 || off2 < 0 || off2 > 9) throw new IllegalArgumentException("REL");
                return c -> relation(c, kind, lag, off, off2); }
            case "XT": { final int off = x.i(0); return c -> c.xt(off); }
            case "XI": {final int e1=x.i(0),e2=x.i(1),off=x.i(2);return c->{
                double[][] a=c.expert(e1),b=c.expert(e2),m=c.xt(off),out=new double[10][10];
                for(int p=0;p<10;p++){int[] sa=top(a[p]),sb=top(b[p]),sm=top(m[p]);for(int n=0;n<10;n++){int votes=contains(sa,n)+contains(sb,n)+contains(sm,n);out[p][n]=clean(votes>=2?votes:0);}}return out;
            };}
            case "HEDGE": {final int window=x.i(0);final double eta=x.i(1)/1000.0;return c->{
                double[] gains=new double[8];int start=Math.max(1,c.t-window);
                for(int u=start;u<c.t;u++){c.budget.checkpointOrYield();Context prior=c.atTarget(u);for(int e=0;e<8;e++){double[][] score=prior.expert(e);double hit=0;for(int p=0;p<10;p++)hit+=contains(top(score[p]),c.draws[u][p]-1);gains[e]=gains[e]*.98+hit/10-.3;}}
                double max=-Double.MAX_VALUE;for(double g:gains)max=Math.max(max,g*eta);double[] weights=new double[8];double total=0;for(int e=0;e<8;e++){weights[e]=Math.exp(gains[e]*eta-max);total+=weights[e];}
                double[][] out=new double[10][10];for(int e=0;e<8;e++){double[][] score=c.expert(e);for(int p=0;p<10;p++)for(int n=0;n<10;n++)out[p][n]+=weights[e]/total*score[p][n];}for(double[] row:out)for(int n=0;n<10;n++)row[n]=clean(row[n]);return out;
            };}
            case "DEEP": {final int width=x.i(0),window=x.i(1),seed=x.i(2);return c->{
                int block=c.t/50*50;String key=width+"|"+window+"|"+seed+"|"+block;
                RelationalStateInference.Weights w=c.models.get(key);
                if(w==null){w=RelationalTrainerSU1.trainCausal(c.draws,block,width,window,seed,c.budget);c.models.put(key,w);while(c.models.size()>8)c.models.remove(c.models.keySet().iterator().next());}
                if(c.t==0)return new double[10][10];double[][] probabilities=RelationalStateInference.predict(w,java.util.Arrays.copyOfRange(c.draws,Math.max(0,c.t-window),c.t));
                for(double[] row:probabilities)for(int n=0;n<10;n++)row[n]=clean(Math.log(Math.max(1e-12,row[n])));return probabilities;
            };}
            case "MODEL": {final String hash=x.s(0);final int window=x.i(1);return c->{
                Fitted fitted=c.fitted.get(hash);
                if(fitted==null)throw new IllegalArgumentException("SU1_FITTED_MODEL_DEPENDENCY_MISSING");
                if(c.t<fitted.firstTarget||c.t<window)throw new IllegalArgumentException("SU1_FITTED_MODEL_TRAINING_BOUNDARY");
                c.budget.checkpointOrYield();double[][] probabilities=RelationalStateInference.predict(fitted.weights,java.util.Arrays.copyOfRange(c.draws,c.t-window,c.t));
                for(double[] row:probabilities)for(int n=0;n<10;n++)row[n]=clean(Math.log(Math.max(1e-12,row[n])));return probabilities;
            };}
            case "FUSE": {final Node left=build(x.t(0)),right=build(x.t(1));final int window=x.i(2);final String identity=x.toString();return c->{
                String key=identity+"|"+(c.t/50);FusionKernelSU1.Model model=c.fusions.get(key);
                if(model==null){model=FusionKernelSU1.train(left,right,c,window);c.fusions.put(key,model);while(c.fusions.size()>8)c.fusions.remove(c.fusions.keySet().iterator().next());}
                return FusionKernelSU1.score(model,left.score(c),right.score(c));
            };}
            case "NUMIS": { final int k = x.i(0); return c -> { double[][] o = new double[10][10];
                for (int p = 0; p < 10; p++) o[p][k] = 1.0; return o; }; }
            case "ADD": case "SUB": case "MUL": case "MAX": {
                final Node a = build(x.t(0)), b = build(x.t(1)); final String op = x.op;
                return c -> { double[][] u = a.score(c), v = b.score(c), o = new double[10][10];
                    for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                        double r;
                        switch (op) { case "ADD": r = u[p][n] + v[p][n]; break; case "SUB": r = u[p][n] - v[p][n]; break;
                            case "MUL": r = u[p][n] * v[p][n]; break; default: r = Math.max(u[p][n], v[p][n]); }
                        o[p][n] = clean(r);
                    }
                    return o; }; }
            case "NEG": case "TANH": { final Node a = build(x.t(0)); final boolean neg = x.op.equals("NEG");
                return c -> { double[][] u = a.score(c), o = new double[10][10];
                    for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) o[p][n] = clean(neg ? -u[p][n] : Math.tanh(u[p][n]));
                    return o; }; }
            case "RANK": { final Node a = build(x.t(0));
                return c -> { double[][] u = a.score(c), o = new double[10][10];
                    for (int p = 0; p < 10; p++) { Integer[] idx = new Integer[10];
                        for (int n = 0; n < 10; n++) idx[n] = n;
                        final double[] row = u[p];
                        java.util.Arrays.sort(idx, (i1, i2) -> Double.compare(row[i1], row[i2])); // stable merge sort
                        for (int r = 0; r < 10; r++) o[p][idx[r]] = clean(r / 9.0); }
                    return o; }; }
            case "IF": { final Cond k = cond(x.t(0)); final Node a = build(x.t(1)), b = build(x.t(2));
                return c -> { double[][] u = a.score(c), v = b.score(c), o = new double[10][];
                    for (int p = 0; p < 10; p++) o[p] = (k.test(c, p) ? u[p] : v[p]).clone();
                    return o; }; }
            case "ATOM": {final String family=x.s(0),params=x.s(1);return c->{
                if(c.atoms==null)throw new IllegalArgumentException("SU1_ATOM_CONTEXT_REQUIRED");
                return c.atoms.score(family,params,c.t,c.budget);
            };}
            default: throw new IllegalArgumentException("UNKNOWN_OP " + x.op);
        }
    }

    private static Cond cond(Term x) {
        switch (x.op) {
            case "GT": { final int q1 = x.i(0), l1 = x.i(1), q2 = x.i(2), l2 = x.i(3);
                return (c, p) -> c.lagValue(q1, l1) > c.lagValue(q2, l2); }
            case "ODD": { final int q = x.i(0), l = x.i(1); return (c, p) -> c.lagValue(q, l) % 2 == 1; }
            case "BIG": { final int q = x.i(0), l = x.i(1); return (c, p) -> c.lagValue(q, l) >= 6; }
            case "HOT": { final int e = x.i(0); return (c, p) -> Experts.hot(c.draws, c.t, e); }
            case "HOTT": { final int w = x.i(0); final Node sub = build(x.t(1));
                // Per position: sub's own top-3 at p hit > 36% over settled rows [t-w, t), and t >= w.
                return (c, p) -> {
                    if (c.t < w) return false;
                    int hits = 0;
                    for (int u = c.t - w; u < c.t; u++) {
                        double[] row = sub.score(c.atTarget(u))[p];
                        Integer[] idx = new Integer[10];
                        for (int n = 0; n < 10; n++) idx[n] = n;
                        java.util.Arrays.sort(idx, (a, b) -> Double.compare(row[b] - b * 1e-6, row[a] - a * 1e-6));
                        int actual = c.draws[u][p] - 1;
                        if (idx[0] == actual || idx[1] == actual || idx[2] == actual) hits++;
                    }
                    return hits / (double) w > 0.36;
                }; }
            case "ATOMC": {final String family=x.s(0),params=x.s(2);final int q=x.i(1);
                return (c,p)->{if(c.atoms==null)throw new IllegalArgumentException("SU1_ATOM_CONTEXT_REQUIRED");return c.atoms.condition(family,q,params,c.t,c.budget);};}
            default: throw new IllegalArgumentException("UNKNOWN_COND " + x.op);
        }
    }

    private static int[] top(double[] row){Integer[] indices=new Integer[10];for(int n=0;n<10;n++)indices[n]=n;java.util.Arrays.sort(indices,(a,b)->Double.compare(row[b]-b*1e-6,row[a]-a*1e-6));return new int[]{indices[0],indices[1],indices[2]};}
    private static int contains(int[] values,int n){for(int v:values)if(v==n)return 1;return 0;}

    private static double[][] relation(Context c, String kind, int lag, int off, int off2) {
        double[][] o = new double[10][10];
        for (int p = 0; p < 10; p++) {
            for (int n = 0; n < 10; n++) o[p][n] = f32((0.0 - 0.1) / 0.3);
            if (c.t < lag) continue;
            int src = c.draws[c.t - lag][(p + off) % 10];
            int tgt;
            switch (kind) {
                case "SAME": tgt = src; break;
                case "PLUS1": tgt = src % 10 + 1; break;
                case "MINUS1": tgt = Math.floorMod(src - 2, 10) + 1; break;
                case "MIRROR": tgt = 11 - src; break;
                case "SUM": tgt = Math.floorMod(src + c.draws[c.t - lag][(p + off + off2) % 10] - 1, 10) + 1; break;
                default: throw new IllegalArgumentException("REL_KIND " + kind);
            }
            o[p][tgt - 1] = f32((1.0 - 0.1) / 0.3);
        }
        return o;
    }

    static double f32(double v) { return (double) (float) v; }

    /** numpy nan_to_num(nan=0, posinf=6, neginf=-6) followed by float32 rounding. */
    static double clean(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v == Double.POSITIVE_INFINITY) return 6.0;
        if (v == Double.NEGATIVE_INFINITY) return -6.0;
        return f32(v);
    }

    // ------------------------------------------------------------------ action head
    /** Returns {position(1..10), n1, n2, n3} in score-rank order; same rule as forge._act_from_scores. */
    public static int[] action(double[][] s) {
        double bestMass = Double.NEGATIVE_INFINITY; int bestP = 0; int[] bestK = null;
        for (int p = 0; p < 10; p++) {
            double mx = Double.NEGATIVE_INFINITY;
            for (int n = 0; n < 10; n++) mx = Math.max(mx, s[p][n]);
            double[] e = new double[10]; double sum = 0;
            for (int n = 0; n < 10; n++) { e[n] = Math.exp(s[p][n] - mx); sum += e[n]; }
            Integer[] idx = new Integer[10];
            for (int n = 0; n < 10; n++) idx[n] = n;
            final double[] row = s[p];
            java.util.Arrays.sort(idx, (a, b) -> Double.compare(row[b] - b * 1e-6, row[a] - a * 1e-6));
            // SU1_ACTION_HEAD_V2: float64 mass rounded to 1e-9; exact ties keep the lowest position.
            double mass = Math.round((e[idx[0]] + e[idx[1]] + e[idx[2]]) / sum * 1e9) / 1e9;
            if (mass > bestMass) { bestMass = mass; bestP = p; bestK = new int[]{idx[0] + 1, idx[1] + 1, idx[2] + 1}; }
        }
        return new int[]{bestP + 1, bestK[0], bestK[1], bestK[2]};
    }

    // ------------------------------------------------------------------ experts (causal)
    static final class Experts {
        /** e: 0..4 frequency windows 12/24/48/96/360, 5 omission, 6 markov720, 7 momentum (r24-r360). */
        static double[][] raw(int[][] d, int t, int e) {
            double[][] v = new double[10][10];
            if (e <= 4) return freq(d, t, WINDOWS[e]);
            if (e == 7) { double[][] a = freq(d, t, 24), b = freq(d, t, 360);
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) v[p][n] = a[p][n] - b[p][n];
                return v; }
            if (e == 5) {
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                    int last = -1;
                    for (int u = t - 1; u >= 0; u--) if (d[u][p] == n + 1) { last = u; break; }
                    v[p][n] = f32(Math.log1p(last >= 0 ? t - last : t + 1));
                }
                return v;
            }
            // markov720: transitions ending at u in [max(1,t-720), t-1], same position
            for (int p = 0; p < 10; p++) {
                if (t < 1) { for (int n = 0; n < 10; n++) v[p][n] = f32(0.1); continue; }
                int prev = d[t - 1][p]; double[] row = new double[10]; double sum = 0;
                for (int u = Math.max(1, t - 720); u <= t - 1; u++) if (d[u - 1][p] == prev) { row[d[u][p] - 1]++; sum++; }
                for (int n = 0; n < 10; n++) v[p][n] = f32((row[n] + 1.0) / (sum + 10.0));
            }
            return v;
        }

        static double[][] freq(int[][] d, int t, int w) {
            double[][] v = new double[10][10];
            int lo = Math.max(t - w, 0), cnt = Math.max(t - lo, 1);
            for (int u = lo; u < t; u++) for (int p = 0; p < 10; p++) v[p][d[u][p] - 1] += 1;
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) v[p][n] = v[p][n] / cnt;   // float64, as numpy
            return v;
        }

        static double[][] znorm(double[][] v) {
            double[][] z = new double[10][10];
            for (int p = 0; p < 10; p++) {
                double mu = 0; for (int n = 0; n < 10; n++) mu += v[p][n]; mu /= 10;
                double var = 0; for (int n = 0; n < 10; n++) var += (v[p][n] - mu) * (v[p][n] - mu); var /= 10;
                double sd = Math.sqrt(var) + 1e-6;
                for (int n = 0; n < 10; n++) z[p][n] = f32((v[p][n] - mu) / sd);
            }
            return z;
        }

        static double[][] z(int[][] d, int t, int e) { return znorm(raw(d, t, e)); }

        static double[][] crossTransition(int[][] d, int t, int off, int window) {
            double[][] v = new double[10][10];
            for (int p = 0; p < 10; p++) {
                int q = (p + off) % 10;
                if (t < 1) { for (int n = 0; n < 10; n++) v[p][n] = f32(0.1); continue; }
                int src = d[t - 1][q]; double[] row = new double[10]; double sum = 0;
                for (int s = Math.max(0, t - 1 - window); s <= t - 2; s++) if (d[s][q] == src) { row[d[s + 1][p] - 1]++; sum++; }
                for (int n = 0; n < 10; n++) v[p][n] = f32((row[n] + 1.0) / (sum + 10.0));
            }
            return znorm(v);
        }

        /** Expert e's settled top-3 hit share over rows [max(t-30,0), t) exceeds 30% (and t >= 30). */
        static boolean hot(int[][] d, int t, int e) {
            if (t < 30) return false;
            double total = 0;
            for (int u = t - 30; u < t; u++) {
                double[][] z = z(d, u, e); int hits = 0;
                for (int p = 0; p < 10; p++) {
                    Integer[] idx = new Integer[10];
                    for (int n = 0; n < 10; n++) idx[n] = n;
                    final double[] row = z[p];
                    java.util.Arrays.sort(idx, (a, b) -> Double.compare(row[b] - b * 1e-6, row[a] - a * 1e-6));
                    int actual = d[u][p] - 1;
                    if (idx[0] == actual || idx[1] == actual || idx[2] == actual) hits++;
                }
                total += hits / 10.0;
            }
            return total / 30.0 > 0.3;
        }
    }
}
