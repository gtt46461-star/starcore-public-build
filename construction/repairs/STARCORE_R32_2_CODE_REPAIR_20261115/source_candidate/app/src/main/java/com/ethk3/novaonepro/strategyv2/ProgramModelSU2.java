package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * SU1 grammar programs (including the 168 lifted atoms of the 25,350-atom registry and
 * the user's XI selector) as members of SU2 strategies (P10/P18): P = softmax(tau * score).
 * They can then be pooled with the mathematical models, gated by the applicability
 * learner and certified selectively. Parameters: text_hex = UTF-8 hex of the canonical
 * SU1 program (hex keeps SU2 delimiters out of the program text), tau.
 * Programs needing fitted assets (DEEP/GRAPH) are rejected here: those stay SU1 members.
 */
public final class ProgramModelSU2 implements TemporalModelR26 {
    final ProgramTreeSU1.Node program; final double tau; final List<int[]> history = new ArrayList<>();

    public static void register() { ModelReplaySU2.ADAPTERS.put("SU1PROG", p -> new ProgramModelSU2(unhex(p.get("text_hex")), Double.parseDouble(p.get("tau")))); }

    public static String hex(String text) { StringBuilder b = new StringBuilder(); for (byte v : text.getBytes(StandardCharsets.UTF_8)) b.append(String.format(Locale.ROOT, "%02x", v & 255)); return b.toString(); }
    static String unhex(String h) { if (h == null || h.length() % 2 != 0 || h.length() > 2048) throw new IllegalArgumentException("SU2_PROGRAM_HEX"); byte[] b = new byte[h.length() / 2]; for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(h.substring(2 * i, 2 * i + 2), 16); return new String(b, StandardCharsets.UTF_8); }

    public ProgramModelSU2(String text, double tau) {
        if (text.contains("DEEP(") || text.contains("GDEEP(") || text.contains("GRAPH(")) throw new IllegalArgumentException("SU2_PROGRAM_NEEDS_FITTED_ASSET");
        if (!(tau > 0 && tau <= 5)) throw new IllegalArgumentException("SU2_PROGRAM_TAU");
        program = ProgramTreeSU1.parse(ProgramTextSU1.canonical(text)); this.tau = tau;
    }
    public void resetTemporalContext(){history.clear();}
    public int stateWidth() { return 3; }
    public void predict(float[][] p, float[] st) {
        int[][] h = history.toArray(new int[0][]);
        double[][] s;
        if (h.length < 30) { s = new double[10][10]; }
        else { try (AtomLiftSU1 atoms = new AtomLiftSU1(h)) { s = program.score(new ProgramTreeSU1.Context(h, h.length, atoms, new ScoreHistoryCacheSU1(256), ExecutionBudgetR26.current()==null?new BudgetTokenSU1(250,3_000_000,null):ExecutionBudgetR26.current())); } }
        double pm = 0, spread = 0;
        for (int q = 0; q < 10; q++) {
            double mx = Double.NEGATIVE_INFINITY, mn = Double.POSITIVE_INFINITY; for (double v : s[q]) { mx = Math.max(mx, v); mn = Math.min(mn, v); }
            double z = 0; double[] e = new double[10]; for (int n = 0; n < 10; n++) { e[n] = Math.exp(tau * (s[q][n] - mx)); z += e[n]; }
            double best = 0; for (int n = 0; n < 10; n++) { p[q][n] = (float) (e[n] / z); best = Math.max(best, e[n] / z); }
            pm += best; spread += mx - mn;
        }
        st[0] = (float) (pm / 10); st[1] = (float) (spread / 10); st[2] = (float) Math.min(1.0, h.length / 5000.0);
    }
    public void update(int[] draw) { OnlineModelsSU2.requireDraw(draw); history.add(draw.clone()); if (history.size() > 5000) history.remove(0); }
    public void write(DataOutput o) throws IOException { o.writeInt(history.size()); for (int[] d : history) for (int v : d) o.writeByte(v); }
    public void read(DataInput in) throws IOException { int n = in.readInt(); if (n < 0 || n > 6500) throw new IOException("SU2_PROGRAM_STATE"); history.clear(); for (int i = 0; i < n; i++) { int[] d = new int[10]; for (int j = 0; j < 10; j++) d[j] = in.readByte(); if(i>=n-5000)history.add(d); } }
}
