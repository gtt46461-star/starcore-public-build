package com.ethk3.novaonepro.strategyv2;

import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** CalibrationSU2: batch == Python foundry14.calibrate (<=1e-6) and live (one period at a time) == batch exactly. */
public final class CalibrationSU2Test {
    static float[] read(Path p) throws Exception { FloatBuffer fb = ByteBuffer.wrap(Files.readAllBytes(p)).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer(); float[] o = new float[fb.remaining()]; fb.get(o); return o; }
    public static void main(String[] a) throws Exception {
        Path r13 = Paths.get(a[0]), r14 = Paths.get(a[1]);
        List<String> lines = Files.readAllLines(r13.resolve("draws.txt")); int T = lines.size(); int[][] d = new int[T][10];
        for (int t = 0; t < T; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) d[t][p] = Integer.parseInt(f[p]); }
        boolean ok = true;
        for (String kind : new String[]{"MSDIR", "BOCPD", "HMM", "LOGLIN", "KALMAN", "RELBAYES"}) {
            float[] raw = read(r13.resolve(kind + ".P.f32")), ref = read(r14.resolve(kind + ".CAL.f32"));
            float[][][] P = new float[T][10][10]; for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) P[t][p][n] = raw[(t * 10 + p) * 10 + n];
            long t0 = System.nanoTime(); float[][][] C = CalibrationSU2.calibrate(P, d); double ms = (System.nanoTime() - t0) / 1e6;
            double dp = 0; for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) dp = Math.max(dp, Math.abs(C[t][p][n] - ref[(t * 10 + p) * 10 + n]));
            CalibrationSU2.Live live = new CalibrationSU2.Live(); double dl = 0;
            for (int t = 0; t < T; t++) { float[][] q = live.predict(P[t]); for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) dl = Math.max(dl, Math.abs(q[p][n] - C[t][p][n])); live.settle(P[t], d[t]); }
            boolean pass = dp <= 1e-6 && dl == 0;
            ok &= pass;
            System.out.printf(Locale.ROOT, "%-9s batch-vs-python maxAbs=%.2e  live-vs-batch maxAbs=%.1e  final beta=%.3f  %.0f ms %s%n", kind, dp, dl, live.beta(), ms, pass ? "PASS" : "FAIL");
        }
        // structured world (beta genuinely > 0 once the model has learned): REGIME seed 1, 9,000 draws, reference = last 1,000
        List<String> wl = Files.readAllLines(r14.resolve("REGIME9000_draws.txt")); int W = wl.size(); int[][] wd = new int[W][10];
        for (int t = 0; t < W; t++) { String[] f = wl.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) wd[t][p] = Integer.parseInt(f[p]); }
        float[] raw = read(r14.resolve("REGIME9000_HMM.P.f32")), ref = read(r14.resolve("REGIME9000_HMM.CAL_LAST1000.f32"));
        float[][][] P = new float[W][10][10]; for (int t = 0; t < W; t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) P[t][p][n] = raw[(t * 10 + p) * 10 + n];
        float[][][] C = CalibrationSU2.calibrate(P, wd);
        double dp = 0; for (int t = W - 1000; t < W; t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) dp = Math.max(dp, Math.abs(C[t][p][n] - ref[((t - (W - 1000)) * 10 + p) * 10 + n]));
        CalibrationSU2.Live live = new CalibrationSU2.Live(); double dl = 0;
        for (int t = 0; t < W; t++) { float[][] q = live.predict(P[t]); for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) dl = Math.max(dl, Math.abs(q[p][n] - C[t][p][n])); live.settle(P[t], wd[t]); }
        boolean pass = dp <= 1e-6 && dl == 0 && live.beta() > 0.1;
        ok &= pass;
        System.out.printf(Locale.ROOT, "REGIME HMM batch-vs-python maxAbs=%.2e  live-vs-batch maxAbs=%.1e  beta=%.3f %s%n", dp, dl, live.beta(), pass ? "PASS" : "FAIL");
        System.out.println(ok ? "R14_CALIBRATION_PASS" : "R14_CALIBRATION_FAIL");
        if (!ok) System.exit(1);
    }
}
