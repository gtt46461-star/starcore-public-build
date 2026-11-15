package com.ethk3.novaonepro.strategyv2;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** R27-010/011 semantic ABI (docs/08). Evidence belongs to a computation, not to a source-tree hash: a policy's
 * semantic fingerprint is SHA-256 over its walk-forward outputs on the fixed 512-period golden set (probability
 * bits ‖ selector output ‖ every-64-row online-state hash). After an upgrade the fingerprint is recomputed with the
 * new code: identical → EQUIVALENT (FUTURE samples, e-process, calibration and certificates continue); different →
 * CHANGED (old evidence frozen, the policy is reborn with operator ABI_REBIRTH) and the first differing issue/field
 * is reported. The operator ABI (OperatorGoldenR27) is the set of per-operator golden fingerprints, not a source hash,
 * so UI/report/scheduler changes never trigger a rebirth. */
public final class SemanticAbiR27 {
    private SemanticAbiR27() {}
    public static final String GOLDEN_ASSET = "r27/golden_512.bin";
    /** SHA-256 of assets/r27/golden_512.bin; changing it requires the docs/08 8.5 procedure. */
    public static final String GOLDEN_SHA256 = "3cd1f137d11a71459f14a8323918eecf6a00a5f533c0c798380770e231e6d913";
    public static final String EQUIVALENT = "EQUIVALENT", CHANGED = "CHANGED", ERROR = "ERROR";

    /** Reads the golden set: magic "R27G", int rows, then per row long ordinal + 10 bytes (numbers 1..10). */
    public static ReplayDatasetR27 readGolden(InputStream in, boolean verifyHash) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
        while ((n = in.read(b)) > 0) buf.write(b, 0, n);
        byte[] bytes = buf.toByteArray();
        if (verifyHash && !GOLDEN_SHA256.equals(StatsR27.sha256(bytes))) throw new IOException("R27_GOLDEN_HASH_MISMATCH");
        DataInputStream d = new DataInputStream(new java.io.ByteArrayInputStream(bytes));
        if (d.readInt() != 0x52323747) throw new IOException("R27_GOLDEN_MAGIC");
        int rows = d.readInt(); List<ReplayDatasetR27.Row> list = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            long ord = d.readLong(); int[] p = new int[10]; for (int j = 0; j < 10; j++) p[j] = d.readUnsignedByte();
            list.add(new ReplayDatasetR27.Row("G" + ord, ord, p));
        }
        return ReplayDatasetR27.of(list);
    }

    public static final class Fingerprint {
        public final String digest; public final int[] probRow, selRow, stateRow; public final String[] issues;
        Fingerprint(String digest, int[] p, int[] s, int[] st, String[] issues) { this.digest = digest; probRow = p; selRow = s; stateRow = st; this.issues = issues; }
        public byte[] rowsBlob() {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(probRow.length * 12);
            for (int i = 0; i < probRow.length; i++) { bb.putInt(probRow[i]); bb.putInt(selRow[i]); bb.putInt(stateRow[i]); }
            return bb.array();
        }
        public static Fingerprint fromBlob(String digest, byte[] blob, String[] issues) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(blob); int n = blob.length / 12; int[] p = new int[n], s = new int[n], st = new int[n];
            for (int i = 0; i < n; i++) { p[i] = bb.getInt(); s[i] = bb.getInt(); st[i] = bb.getInt(); }
            return new Fingerprint(digest, p, s, st, issues);
        }
    }

    /** Fingerprint of a policy with its live temperature on the golden set. */
    public static Fingerprint fingerprint(PolicySpecR27 spec, double temperature, FeatureCacheR27 golden) {
        ReplayDatasetR27 g = golden.data; int n = g.size();
        ReplayEvaluatorR27.Trace tr = ReplayEvaluatorR27.run(spec, golden, g.draws, temperature, 0, n);
        double[] scores = ReplayEvaluatorR27.scores(spec, golden), z = new double[10], p = new double[10], bias = new double[10];
        int[] pr = new int[n], sr = new int[n], st = new int[n];
        StringBuilder all = new StringBuilder("R27_SEMANTIC_FP_V1|").append(spec.policyHash()).append('|').append(Double.toHexString(temperature));
        for (int t = 0; t < n; t++) {
            StringBuilder row = new StringBuilder();
            if (ReplayEvaluatorR27.standardize(scores, t, z)) {
                ReplayEvaluatorR27.softmax(z, temperature, bias, p);
                for (double x : p) row.append(Long.toHexString(Double.doubleToLongBits(x))).append(',');
                int y = g.draws[t][spec.position - 1] - 1;
                for (int k = 0; k < 10; k++) bias[k] = bias[k] * (1 - spec.forget) + spec.lr * ((k == y ? 1 : 0) - p[k]);
            } else row.append("NA");
            pr[t] = row.toString().hashCode();
            String sel = tr.skipped[t] ? "SKIP" : (tr.act[t] ? "A" : "H") + tr.k3[t * 3] + tr.k3[t * 3 + 1] + tr.k3[t * 3 + 2];
            sr[t] = sel.hashCode();
            StringBuilder state = new StringBuilder();
            if ((t + 1) % 64 == 0) for (double x : bias) state.append(Long.toHexString(Double.doubleToLongBits(x))).append(',');
            st[t] = state.toString().hashCode();
            all.append('|').append(row).append(';').append(sel).append(';').append(state);
        }
        return new Fingerprint(StatsR27.sha256(all.toString()), pr, sr, st, g.issues);
    }

    public static final class Result { public final String result, firstDiffIssue, firstDiffField; Result(String r, String i, String f) { result = r; firstDiffIssue = i; firstDiffField = f; } }

    /** Compares a stored fingerprint with a recomputed one; reports the first differing period and field. */
    public static Result compare(Fingerprint old, Fingerprint now) {
        if (old.digest.equals(now.digest)) return new Result(EQUIVALENT, null, null);
        int n = Math.min(old.probRow.length, now.probRow.length);
        for (int t = 0; t < n; t++) {
            if (old.probRow[t] != now.probRow[t]) return new Result(CHANGED, now.issues[t], "probabilities");
            if (old.selRow[t] != now.selRow[t]) return new Result(CHANGED, now.issues[t], "selector");
            if (old.stateRow[t] != now.stateRow[t]) return new Result(CHANGED, now.issues[t], "state");
        }
        return new Result(CHANGED, null, "length");
    }

    /** Operators/leaves used by a policy (the part of the operator ABI it depends on). */
    public static TreeSet<String> operators(PolicySpecR27 spec) {
        TreeSet<String> s = new TreeSet<>();
        for (PolicyAstR27.Node n : PolicyAstR27.nodes(spec.ast)) s.add(n.op.equals("family") ? "family:" + n.ints[0] : n.op);
        s.add("readout:softmax"); s.add("selector:" + spec.rule); s.add("updater:bias_sgd");
        return s;
    }

    /** Semantic ABI string of a policy = its operators with their golden fingerprints. */
    public static String policyAbi(PolicySpecR27 spec, Map<String, String> operatorAbi) {
        StringBuilder b = new StringBuilder();
        for (String op : operators(spec)) b.append(op).append('=').append(operatorAbi.getOrDefault(op, "?")).append(';');
        return StatsR27.sha256(b.toString());
    }

    /** Hash of the whole operator ABI map (bound as r26_capability_source instead of the source-tree training hash). */
    public static String operatorSetHash(Map<String, String> operatorAbi) {
        StringBuilder b = new StringBuilder("R27_OPERATOR_ABI_V1"); for (Map.Entry<String, String> e : new TreeMap<>(operatorAbi).entrySet()) b.append('|').append(e.getKey()).append('=').append(e.getValue());
        return StatsR27.sha256(b.toString());
    }

    public static Map<String, Object> json(Result r) { Map<String, Object> m = new LinkedHashMap<>(); m.put("result", r.result); m.put("firstDiffIssue", r.firstDiffIssue); m.put("firstDiffField", r.firstDiffField); return m; }
}
