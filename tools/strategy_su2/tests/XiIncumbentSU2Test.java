package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** The Java incumbent reproduces the Python reference of the user's XI method on the 5.4.3 report draws. */
public final class XiIncumbentSU2Test {
    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[0]);
        List<String> lines = Files.readAllLines(dir.resolve("draws.txt")); int[][] d = new int[lines.size()][10];
        for (int t = 0; t < d.length; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) d[t][p] = Integer.parseInt(f[p]); }
        List<String> ref = Files.readAllLines(dir.resolve("xi_reference.txt"));
        String act = ref.get(0).trim(), hit = ref.get(1).trim(), pos = ref.get(2).trim();
        long t0 = System.currentTimeMillis();
        XiIncumbentSU2.Result r = XiIncumbentSU2.evaluate(d, 1500, d.length);
        int da = 0, dh = 0, dp = 0, n = 0, k = 0, rn = 0, rk = 0;
        for (int t = 1500; t < d.length; t++) {
            boolean ra = act.charAt(t) == '1', rh = hit.charAt(t) == '1';
            if (r.act[t] != ra) da++; if (r.hit[t] != rh) dh++; if (r.position[t] != pos.charAt(t) - '0' + (pos.charAt(t) == ':' ? 0 : 0)) dp++;
            if (r.act[t]) { n++; if (r.hit[t]) k++; } if (ra) { rn++; if (rh) rk++; }
        }
        int T = d.length - 1500;
        System.out.printf(Locale.ROOT, "java acts=%d hits=%d rate=%.4f | python acts=%d hits=%d rate=%.4f | actMismatch=%d (%.2f%%) hitMismatch=%d positionMismatch=%d | %ds%n",
            n, k, k / (double) Math.max(1, n), rn, rk, rk / (double) Math.max(1, rn), da, 100.0 * da / T, dh, dp, (System.currentTimeMillis() - t0) / 1000);
        boolean ok = dh <= T / 200 && da <= T / 50 && Math.abs(k / (double) Math.max(1, n) - rk / (double) Math.max(1, rn)) < 0.015;
        int dn = 0;
        for (int t : new int[]{2600, 3333, 4000, 4444, d.length - 1}) {
            int[] x = XiIncumbentSU2.decideNext(Arrays.copyOf(d, t));
            boolean same = (x[0] == 1) == r.act[t] && x[1] == r.position[t] && x[2] == r.k3[t][0] && x[3] == r.k3[t][1] && x[4] == r.k3[t][2];
            // the full decision (frozen phat for post-selection calibration, pooled table for the certified lane) is the replay's
            XiIncumbentSU2.Decision f = XiIncumbentSU2.decideNextFull(Arrays.copyOf(d, t));
            boolean ph = Double.isNaN(f.phat) ? Double.isNaN(r.prob[t]) : Math.abs(f.phat - r.prob[t]) < 1e-12;
            double rs = 0; for (float v : f.table[f.position - 1]) rs += v;
            if (!same || !ph || Math.abs(rs - 1) > 1e-5 || f.mask() != ((1 << (r.k3[t][0] - 1)) | (1 << (r.k3[t][1] - 1)) | (1 << (r.k3[t][2] - 1)))) dn++;
        }
        System.out.println("decideNext/decideNextFull (act, position, K3, phat, table) == replay at 5 targets: mismatches=" + dn + "  genomeKey=" + XiIncumbentSU2.GENOME_KEY.substring(0, 16) + "...");
        ok &= dn == 0;
        System.out.println(ok ? "XI_INCUMBENT_PARITY_PASS" : "XI_INCUMBENT_PARITY_FAIL");
        if (!ok) System.exit(1);
    }
}
