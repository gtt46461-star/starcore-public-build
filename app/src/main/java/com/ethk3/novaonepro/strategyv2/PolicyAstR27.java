package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** R27-021 scoring program AST (docs/04 4.2). Immutable nodes, canonical S-expression text, vectorized
 * point-in-time evaluation over a whole replay dataset (one double[T*10] per node), and a small type system
 * (PROB / FLAG / RISK / SCORE / CONST) so that probabilities, indicators and risks are not silently mixed. */
public final class PolicyAstR27 {
    private PolicyAstR27() {}

    public enum Type { PROB, FLAG, RISK, SCORE, CONST }

    /** Leaf name -> {int-parameter count, result type}. */
    static final Map<String, Object[]> LEAVES = new HashMap<>();
    static final Map<String, Integer> ARITY = new HashMap<>();
    static {
        Object[][] leaves = {
            {"freq", 2, Type.PROB}, {"fdecay", 2, Type.PROB}, {"freqnb", 2, Type.PROB}, {"gap", 1, Type.RISK}, {"gaprank", 1, Type.RISK},
            {"meangap", 2, Type.RISK}, {"trans1", 0, Type.PROB}, {"trans2", 0, Type.PROB}, {"cross", 2, Type.FLAG}, {"crosscond", 1, Type.PROB},
            {"repeat", 0, Type.FLAG}, {"neighbor", 1, Type.FLAG}, {"oddeven", 2, Type.PROB}, {"bigsmall", 2, Type.PROB}, {"sumbucket", 0, Type.PROB},
            {"regime", 1, Type.RISK}, {"knn", 2, Type.PROB}, {"family", 1, Type.PROB}, {"model_surface",1,Type.PROB}, {"randu", 1, Type.SCORE}};
        for (Object[] l : leaves) LEAVES.put((String) l[0], new Object[] {l[1], l[2]});
        for (String u : new String[] {"neg", "abs", "log1p", "sqrt", "sigmoid", "tanh", "rank", "zscore", "lagdiff"}) ARITY.put(u, 1);
        for (String b : new String[] {"add", "sub", "mul", "div", "max", "min"}) ARITY.put(b, 2);
        ARITY.put("softmax", 1); ARITY.put("ema", 1); ARITY.put("mix", 2); ARITY.put("if", 4);
    }
    /** Operators that carry one real parameter (trainable constant). */
    static boolean hasParam(String op) { return op.equals("softmax") || op.equals("ema") || op.equals("mix") || op.equals("c"); }

    public static final class Node {
        public final String op; public final int[] ints; public final double param; public final Node[] kids; public final int modelPositions;
        public Node(String op, int[] ints, double param, Node... kids) { this.op = op; this.ints = ints == null ? new int[0] : ints.clone(); this.param = param; this.kids = kids == null ? new Node[0] : kids.clone(); int mask=op.equals("model_surface")&&this.ints.length==1&&pos(this.ints[0])?1<<(this.ints[0]-1):0;for(Node k:this.kids)mask|=k.modelPositions;modelPositions=mask; }
        public boolean leaf() { return LEAVES.containsKey(op) || op.equals("c"); }
        public int size() { int s = 1; for (Node k : kids) s += k.size(); return s; }
        public int depth() { int d = 0; for (Node k : kids) d = Math.max(d, k.depth()); return d + 1; }
        public int constants() { int s = hasParam(op) ? 1 : 0; for (Node k : kids) s += k.constants(); return s; }
        @Override public String toString() { StringBuilder b = new StringBuilder(); print(this, b); return b.toString(); }
        @Override public boolean equals(Object o) { return o instanceof Node && toString().equals(o.toString()); }
        @Override public int hashCode() { return toString().hashCode(); }
    }

    public static Node leaf(String name, int... ints) { return new Node(name, ints, 0); }
    public static Node constant(double v) { return new Node("c", null, v); }
    public static Node op(String op, Node... kids) { return new Node(op, null, 0, kids); }
    public static Node op(String op, double param, Node... kids) { return new Node(op, null, param, kids); }

    // ------------------------------------------------------------------ text
    static void print(Node n, StringBuilder b) {
        b.append('(').append(n.op);
        if (hasParam(n.op)) b.append(' ').append(num(n.param));
        for (int i : n.ints) b.append(' ').append(i);
        for (Node k : n.kids) { b.append(' '); print(k, b); }
        b.append(')');
    }
    /** Lossless decimal text: Double.toString round-trips exactly; never truncated (R27-021). */
    static String num(double v) { if (!Double.isFinite(v)) throw new IllegalArgumentException("R27_NONFINITE_CONSTANT"); return Double.toString(v); }

    public static Node parse(String text) {
        int[] at = {0}; Node n = parse(text, at); skip(text, at);
        if (at[0] != text.length()) throw new IllegalArgumentException("R27_AST_TRAILING");
        return n;
    }
    private static void skip(String s, int[] at) { while (at[0] < s.length() && Character.isWhitespace(s.charAt(at[0]))) at[0]++; }
    private static String token(String s, int[] at) {
        skip(s, at); int st = at[0];
        while (at[0] < s.length() && !Character.isWhitespace(s.charAt(at[0])) && s.charAt(at[0]) != '(' && s.charAt(at[0]) != ')') at[0]++;
        if (st == at[0]) throw new IllegalArgumentException("R27_AST_TOKEN@" + st);
        return s.substring(st, at[0]);
    }
    private static Node parse(String s, int[] at) {
        skip(s, at);
        if (at[0] >= s.length() || s.charAt(at[0]) != '(') throw new IllegalArgumentException("R27_AST_OPEN@" + at[0]);
        at[0]++;
        String op = token(s, at);
        if (!LEAVES.containsKey(op) && !ARITY.containsKey(op) && !op.equals("c")) throw new IllegalArgumentException("R27_AST_UNKNOWN_OP:" + op);
        double param = 0; List<Integer> ints = new ArrayList<>(); List<Node> kids = new ArrayList<>();
        if (hasParam(op)) param = Double.parseDouble(token(s, at));
        while (true) {
            skip(s, at);
            if (at[0] >= s.length()) throw new IllegalArgumentException("R27_AST_EOF");
            char c = s.charAt(at[0]);
            if (c == ')') { at[0]++; break; }
            if (c == '(') kids.add(parse(s, at)); else ints.add(Integer.parseInt(token(s, at)));
        }
        int[] iv = new int[ints.size()]; for (int i = 0; i < iv.length; i++) iv[i] = ints.get(i);
        return new Node(op, iv, param, kids.toArray(new Node[0]));
    }

    // ------------------------------------------------------------------ checks
    /** Static type. Throws IllegalArgumentException("TYPE:...") on an illegal combination. */
    public static Type type(Node n) {
        if (n.op.equals("c")) return Type.CONST;
        Object[] lf = LEAVES.get(n.op);
        if (lf != null) { if (n.kids.length != 0 || n.ints.length != (Integer) lf[0]) throw new IllegalArgumentException("TYPE:LEAF_SHAPE:" + n.op); return (Type) lf[1]; }
        Integer arity = ARITY.get(n.op);
        if (arity == null || n.kids.length != arity || n.ints.length != 0) throw new IllegalArgumentException("TYPE:ARITY:" + n.op);
        Type[] k = new Type[n.kids.length];
        for (int i = 0; i < k.length; i++) k[i] = type(n.kids[i]);
        switch (n.op) {
            case "add": case "sub": {
                if (k[0] == Type.CONST) return k[1]; if (k[1] == Type.CONST) return k[0];
                if (k[0] == Type.SCORE || k[1] == Type.SCORE) return Type.SCORE;
                if (k[0] != k[1]) throw new IllegalArgumentException("TYPE:MIXED_" + k[0] + "_" + k[1] + "_IN_" + n.op);
                return k[0];
            }
            case "mix": if (k[0] != k[1] && k[0] != Type.SCORE && k[1] != Type.SCORE) throw new IllegalArgumentException("TYPE:MIX_" + k[0] + "_" + k[1]); return k[0] == k[1] ? k[0] : Type.SCORE;
            case "if": if (k[2] != k[3] && k[2] != Type.SCORE && k[3] != Type.SCORE) throw new IllegalArgumentException("TYPE:IF_BRANCH_" + k[2] + "_" + k[3]); return k[2] == k[3] ? k[2] : Type.SCORE;
            case "softmax": if (k[0] == Type.CONST) throw new IllegalArgumentException("TYPE:SOFTMAX_CONST"); return Type.PROB;
            case "log1p": case "sqrt": if (k[0] == Type.SCORE) throw new IllegalArgumentException("TYPE:" + n.op + "_NEEDS_NONNEGATIVE"); return Type.SCORE;
            default: return Type.SCORE;
        }
    }

    /** Parameter ranges (RANGE) and causality (CAUSAL). Returns null when legal, else "RANGE:..."/"CAUSAL:...". */
    public static String rangeAndCausality(Node n) {
        if (hasParam(n.op) && (!Double.isFinite(n.param) || Math.abs(n.param) > 1000)) return "RANGE:PARAM:" + n.op;
        switch (n.op) {
            case "softmax": if (n.param < 0.05 || n.param > 20) return "RANGE:TEMPERATURE"; break;
            case "ema": case "mix": if (n.param < 0 || n.param > 1) return "RANGE:ALPHA"; break;
            case "freq": case "freqnb": case "meangap": case "oddeven": case "bigsmall": if (!pos(n.ints[0]) || !window(n.ints[1])) return "RANGE:" + n.op; break;
            case "fdecay": if (!pos(n.ints[0]) || Arrays.binarySearch(FeatureCacheR27.HALF_LIVES, n.ints[1]) < 0) return "RANGE:fdecay"; break;
            case "model_surface": case "gap": case "gaprank": case "crosscond": case "neighbor": if (!pos(n.ints[0])) return "RANGE:" + n.op; break;
            case "cross": if (!pos(n.ints[0])) return "RANGE:cross"; if (n.ints[1] < 1) return "CAUSAL:LAG_" + n.ints[1]; if (n.ints[1] > 3) return "RANGE:LAG"; break;
            case "regime": if (!window(n.ints[0])) return "RANGE:regime"; break;
            case "knn": if (n.ints[0] < 2 || n.ints[0] > 6 || n.ints[1] < 5 || n.ints[1] > 60) return "RANGE:knn"; break;
            case "family": if (!FAMILY_LEAVES.contains(n.ints[0])) return "RANGE:family"; break;
            default: break;
        }
        for (Node k : n.kids) { String r = rangeAndCausality(k); if (r != null) return r; }
        return null;
    }
    static boolean pos(int p) { return p >= 1 && p <= 10; }
    static boolean window(int w) { return Arrays.binarySearch(FeatureCacheR27.WINDOWS, w) >= 0; }
    /** R26 families exposed as AST leaves (asLeaf). F01/F02/F09/F17 measured 2.4–7 ms per period on a PC (above the
     * 5 ms birth cost gate or the per-generation budget on a phone), so they stay in the R26 foundry, not in the AST. */
    public static final List<Integer> FAMILY_LEAVES = Collections.unmodifiableList(Arrays.asList(8, 11, 12, 20, 21));

    // ------------------------------------------------------------------ evaluation
    /** Vectorized PIT evaluation for target position tp: returns double[T*10] (NaN = unavailable). */
    public static double[] eval(Node n, FeatureCacheR27 f, int tp) {
        double[] values=evalNode(n,f,tp);
        if(n.modelPositions!=0)f.maskMissingModelRows(values,n.modelPositions);
        return values;
    }
    private static double[] evalNode(Node n,FeatureCacheR27 f,int tp) {
        int len = f.data.size() * 10;
        if (n.op.equals("c")) { double[] v = new double[len]; Arrays.fill(v, n.param); return v; }
        if (LEAVES.containsKey(n.op)) {
            int a = n.ints.length > 0 ? n.ints[0] : 0, b = n.ints.length > 1 ? n.ints[1] : 0;
            if (n.op.equals("regime")) { b = a; a = 0; }
            return f.leaf(n.op, tp, a, b);
        }
        double[][] k = new double[n.kids.length][];
        for (int i = 0; i < k.length; i++) k[i] = eval(n.kids[i], f, tp);
        double[] v = new double[len];
        double p = n.param;
        switch (n.op) {
            case "neg": for (int i = 0; i < len; i++) v[i] = -k[0][i]; break;
            case "abs": for (int i = 0; i < len; i++) v[i] = Math.abs(k[0][i]); break;
            case "log1p": for (int i = 0; i < len; i++) v[i] = Math.log1p(Math.abs(k[0][i])); break;
            case "sqrt": for (int i = 0; i < len; i++) v[i] = Math.sqrt(Math.abs(k[0][i])); break;
            case "sigmoid": for (int i = 0; i < len; i++) v[i] = 1 / (1 + Math.exp(-k[0][i])); break;
            case "tanh": for (int i = 0; i < len; i++) v[i] = Math.tanh(k[0][i]); break;
            case "add": for (int i = 0; i < len; i++) v[i] = k[0][i] + k[1][i]; break;
            case "sub": for (int i = 0; i < len; i++) v[i] = k[0][i] - k[1][i]; break;
            case "mul": for (int i = 0; i < len; i++) v[i] = k[0][i] * k[1][i]; break;
            case "div": for (int i = 0; i < len; i++) { double d = k[1][i]; v[i] = Math.abs(d) < 1e-9 ? 0 : k[0][i] / d; } break;
            case "max": for (int i = 0; i < len; i++) v[i] = Math.max(k[0][i], k[1][i]); break;
            case "min": for (int i = 0; i < len; i++) v[i] = Math.min(k[0][i], k[1][i]); break;
            case "mix": for (int i = 0; i < len; i++) v[i] = p * k[0][i] + (1 - p) * k[1][i]; break;
            case "if": for (int i = 0; i < len; i++) v[i] = k[0][i] > k[1][i] ? k[2][i] : k[3][i]; break;
            case "rank": rowWise(k[0], v, 0, 0); break;
            case "zscore": rowWise(k[0], v, 1, 0); break;
            case "softmax": rowWise(k[0], v, 2, p); break;
            case "lagdiff": {
                Arrays.fill(v, Double.NaN);
                for (int t = 1; t < f.data.size(); t++) if (f.data.consecutive(t, 1)) for (int j = 0; j < 10; j++) v[t * 10 + j] = k[0][t * 10 + j] - k[0][(t - 1) * 10 + j];
                break;
            }
            case "ema": {
                double[] e = new double[10]; boolean[] init = new boolean[10];
                for (int t = 0; t < f.data.size(); t++) for (int j = 0; j < 10; j++) {
                    double x = k[0][t * 10 + j];
                    if (Double.isNaN(x)) { v[t * 10 + j] = init[j] ? e[j] : Double.NaN; continue; }
                    e[j] = init[j] ? p * x + (1 - p) * e[j] : x; init[j] = true; v[t * 10 + j] = e[j];
                }
                break;
            }
            default: throw new IllegalArgumentException("R27_AST_EVAL:" + n.op);
        }
        return v;
    }

    /** mode 0 rank/9, 1 z-score, 2 softmax(temperature). A row with any NaN stays NaN. */
    private static void rowWise(double[] x, double[] v, int mode, double temperature) {
        for (int t = 0; t < x.length / 10; t++) {
            int o = t * 10; boolean bad = false;
            for (int j = 0; j < 10; j++) if (Double.isNaN(x[o + j])) bad = true;
            if (bad) { for (int j = 0; j < 10; j++) v[o + j] = Double.NaN; continue; }
            if (mode == 0) {
                for (int j = 0; j < 10; j++) { int r = 0; for (int i = 0; i < 10; i++) if (x[o + i] < x[o + j] || (x[o + i] == x[o + j] && i < j)) r++; v[o + j] = r / 9.0; }
            } else if (mode == 1) {
                double m = 0, s = 0; for (int j = 0; j < 10; j++) m += x[o + j]; m /= 10;
                for (int j = 0; j < 10; j++) s += (x[o + j] - m) * (x[o + j] - m); s = Math.sqrt(s / 10);
                for (int j = 0; j < 10; j++) v[o + j] = s < 1e-12 ? 0 : (x[o + j] - m) / s;
            } else {
                double mx = Double.NEGATIVE_INFINITY; for (int j = 0; j < 10; j++) mx = Math.max(mx, x[o + j] * temperature);
                double z = 0; for (int j = 0; j < 10; j++) { v[o + j] = Math.exp(x[o + j] * temperature - mx); z += v[o + j]; }
                for (int j = 0; j < 10; j++) v[o + j] /= z;
            }
        }
    }

    // ------------------------------------------------------------------ structure helpers for operators
    /** Pre-order list of all nodes (paths are indices into this list). */
    public static List<Node> nodes(Node root) { List<Node> out = new ArrayList<>(); collect(root, out); return out; }
    private static void collect(Node n, List<Node> out) { out.add(n); for (Node k : n.kids) collect(k, out); }
    /** Replaces the pre-order node at index target by replacement. */
    public static Node replace(Node root, int target, Node replacement) { int[] at = {0}; return replace(root, target, replacement, at); }
    private static Node replace(Node n, int target, Node r, int[] at) {
        if (at[0]++ == target) return r;
        Node[] kids = new Node[n.kids.length]; boolean changed = false;
        for (int i = 0; i < kids.length; i++) { kids[i] = replace(n.kids[i], target, r, at); changed |= kids[i] != n.kids[i]; }
        return changed ? new Node(n.op, n.ints, n.param, kids) : n;
    }
    public static Node withParam(Node n, double p) { return new Node(n.op, n.ints, p, n.kids); }
    public static Node withInts(Node n, int... ints) { return new Node(n.op, ints, n.param, n.kids); }

    /** Main information source class for the MAP-Elites descriptor (docs/05 5.2). */
    public static int sourceClass(Node root) {
        int[] c = new int[8];
        for (Node n : nodes(root)) {
            switch (n.op) {
                case "freq": case "fdecay": case "freqnb": case "oddeven": case "bigsmall": c[0]++; break;
                case "gap": case "gaprank": case "meangap": c[1]++; break;
                case "trans1": case "trans2": case "repeat": case "sumbucket": c[2]++; break;
                case "cross": case "crosscond": case "neighbor": c[3]++; break;
                case "regime": case "if": c[4]++; break;
                case "knn": c[5]++; break;
                case "family": c[6]++; break;
                case "mix": c[7]++; break;
                default: break;
            }
        }
        int best = 0; for (int i = 1; i < 8; i++) if (c[i] > c[best]) best = i;
        return best;
    }
    public static final String[] SOURCE_NAMES = {"频率", "遗漏", "转移", "跨位置", "状态", "检索", "神经/家族", "组合"};

    /** Chinese rendering for the lineage dialog (docs/04 4.7 readability). */
    public static String describe(Node n) {
        switch (n.op) {
            case "c": return num(n.param);
            case "freq": return "第" + n.ints[0] + "位近" + n.ints[1] + "期频率";
            case "fdecay": return "第" + n.ints[0] + "位衰减频率(半衰期" + n.ints[1] + ")";
            case "freqnb": return "第" + n.ints[0] + "位邻位近" + n.ints[1] + "期频率";
            case "gap": return "第" + n.ints[0] + "位遗漏";
            case "gaprank": return "第" + n.ints[0] + "位遗漏排名";
            case "meangap": return "第" + n.ints[0] + "位近" + n.ints[1] + "期平均间隔";
            case "trans1": return "本位一阶转移";
            case "trans2": return "本位二阶转移";
            case "cross": return "第" + n.ints[0] + "位滞后" + n.ints[1] + "期号码";
            case "crosscond": return "第" + n.ints[0] + "位→本位条件转移";
            case "repeat": return "上期本位重复";
            case "neighbor": return "第" + n.ints[0] + "位上期邻号";
            case "oddeven": return "第" + n.ints[0] + "位奇偶同类率";
            case "bigsmall": return "第" + n.ints[0] + "位大小同类率";
            case "sumbucket": return "前三位和值条件分布";
            case "regime": return "近" + n.ints[0] + "期重复体制";
            case "knn": return "相似片段(W=" + n.ints[0] + ",k=" + n.ints[1] + ")";
            case "family": return String.format(Locale.ROOT, "家族F%02d", n.ints[0]);
            case "randu": return "随机基线噪声";
            default: {
                StringBuilder b = new StringBuilder(n.op);
                if (hasParam(n.op)) b.append('[').append(num(n.param)).append(']');
                b.append('(');
                for (int i = 0; i < n.kids.length; i++) { if (i > 0) b.append(", "); b.append(describe(n.kids[i])); }
                return b.append(')').toString();
            }
        }
    }
}
