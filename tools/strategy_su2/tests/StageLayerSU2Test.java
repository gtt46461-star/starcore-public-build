package com.ethk3.novaonepro.strategyv2;

import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * P49 stage layer against the Python reference (fixture r16_stage: 9,000 REGIME draws, three calibrated candidates):
 * fingerprint rows at sample periods, the chosen number of stages and BIC, P(hit) of every candidate on [7000, 9000)
 * after a fit on [1500, 7000); and the live one-period-at-a-time path equal to the batch path.
 * Prints R16_STAGE_PARITY_PASS.
 */
public final class StageLayerSU2Test {
    static double[] f64(Path p) throws Exception { byte[] b = Files.readAllBytes(p); DoubleBuffer d = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer(); double[] o = new double[d.remaining()]; d.get(o); return o; }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r16_stage");
        int[][] D = AtomNetSU2Test.draws(dir.resolve("draws.txt")); int T = D.length, S = 3;
        double[] cf = f64(dir.resolve("conf.f64")), st0 = f64(dir.resolve("st0.f64")), FS = f64(dir.resolve("F_sample.f64")), PP = f64(dir.resolve("P.f64"));
        byte[] hb = Files.readAllBytes(dir.resolve("hit.u8"));
        double[][] confs = new double[S][T]; boolean[][] hits = new boolean[S][T];
        for (int s = 0; s < S; s++) for (int t = 0; t < T; t++) { confs[s][t] = cf[s * T + t]; hits[s][t] = hb[s * T + t] != 0; }
        long t0 = System.nanoTime();
        double[][] F = StageLayerSU2.fingerprints(D, hits, st0);
        List<Integer> sample = new ArrayList<>(); for (String l : Files.readAllLines(dir.resolve("sample_t.txt"))) if (!l.trim().isEmpty()) sample.add(Integer.parseInt(l.trim()));
        int d = F[0].length; double wf = 0;
        for (int i = 0; i < sample.size(); i++) for (int j = 0; j < d; j++) wf = Math.max(wf, Math.abs(F[sample.get(i)][j] - FS[i * d + j]) / Math.max(1, Math.abs(FS[i * d + j])));
        StageLayerSU2.Layer L = StageLayerSU2.fit(F, confs, hits, 1500, 7000);
        String[] st = Files.readAllLines(dir.resolve("stages.txt")).get(0).trim().split(" ");
        int Kpy = Integer.parseInt(st[0]); double bicPy = Double.parseDouble(st[1]);
        double[][] P = StageLayerSU2.apply(L, F, confs, 7000, 9000);
        double wp = 0; int argDiff = 0;
        for (int u = 0; u < 2000; u++) {
            int aj = 0, ap = 0;
            for (int s = 0; s < S; s++) { wp = Math.max(wp, Math.abs(P[u][s] - PP[u * S + s])); if (P[u][s] > P[u][aj]) aj = s; if (PP[u * S + s] > PP[u * S + ap]) ap = s; }
            if (aj != ap) argDiff++;
        }
        // live path == batch path
        StageLayerSU2.Live live = new StageLayerSU2.Live(L); double wl = 0;
        for (int u = 7000; u < 9000; u++) {
            double[] perf = new double[S]; for (int s = 0; s < S; s++) { int k = 0; for (int v = u - 50; v < u; v++) if (hits[s][v]) k++; perf[s] = k / 50.0; }
            double[] row = StageLayerSU2.row(D, u, st0[u], perf);
            double[] q = live.step(row, new double[]{confs[0][u], confs[1][u], confs[2][u]});
            for (int s = 0; s < S; s++) wl = Math.max(wl, Math.abs(q[s] - P[u - 7000][s]));
        }
        System.out.printf(Locale.ROOT, "stage layer: fingerprint rel diff %.2e; stages java %d python %d; BIC java %.6f python %.6f; max|dP| %.2e; best-candidate disagreements %d/2000; live vs batch %.2e; %.1f s%n",
            wf, L.stages(), Kpy, L.gmm.bic, bicPy, wp, argDiff, wl, (System.nanoTime() - t0) / 1e9);
        boolean ok = wf < 1e-9 && L.stages() == Kpy && Math.abs(L.gmm.bic - bicPy) / Math.abs(bicPy) < 1e-9 && wp < 1e-6 && argDiff == 0 && wl == 0;
        System.out.println(ok ? "R16_STAGE_PARITY_PASS" : "R16_STAGE_PARITY_FAIL");
        if (!ok) System.exit(1);
    }
}
