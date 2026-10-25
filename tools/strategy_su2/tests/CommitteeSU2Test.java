package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** R14 parity: calibrated programs (MASS / normalised STATIC / BMA+Sinkhorn / META) and committees, Java == Python foundry14. */
public final class CommitteeSU2Test {
    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[0]);
        List<String> lines = Files.readAllLines(dir.resolve("REGIME9000_draws.txt")); int T = lines.size(); int[][] d = new int[T][10];
        for (int t = 0; t < T; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) d[t][p] = Integer.parseInt(f[p]); }
        String json = new String(Files.readAllBytes(dir.resolve("programs.json")), "UTF-8");
        Matcher m = Pattern.compile("\"name\": \"(\\w+)\",\\s*\"text\": \"([^\"]+)\",\\s*\"act\": \"([01]+)\",\\s*\"hit\": \"([01]+)\",\\s*\"position\": \"(\\d+)\"").matcher(json);
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 768L << 20);
        boolean ok = true; int n = 0;
        while (m.find()) {
            n++; String name = m.group(1), text = m.group(2), act = m.group(3), hit = m.group(4), pos = m.group(5);
            long t0 = System.nanoTime();
            ModelReplaySU2.Result r;
            if (CommitteeSU2.isCommittee(text)) { CommitteeSU2 c = CommitteeSU2.parse(text); if (!c.text().equals(text)) throw new AssertionError("committee text"); r = CommitteeSU2.evaluate(c, lib, 1500); }
            else { ModelGenomeSU2 g = ModelGenomeSU2.parse(text); if (!g.calibrated) throw new AssertionError("CAL flag"); r = ModelReplaySU2.evaluate(g, lib, 1500, 2500, 250); }
            int da = 0, dh = 0, dp = 0, acts = 0, hits = 0;
            for (int t = 0; t < T; t++) {
                if (r.act[t] != (act.charAt(t) == '1')) da++;
                if (r.hit[t] != (hit.charAt(t) == '1')) dh++;
                if (r.position[t] - 1 != pos.charAt(t) - '0') dp++;
                if (t >= 1500 && r.act[t]) { acts++; if (r.hit[t]) hits++; }
            }
            boolean pass = da == 0 && dh == 0 && dp == 0;
            ok &= pass;
            System.out.printf(Locale.ROOT, "%-3s acts=%d hits=%d actMismatch=%d hitMismatch=%d positionMismatch=%d %.0f ms %s  %s%n", name, acts, hits, da, dh, dp,
                (System.nanoTime() - t0) / 1e6, pass ? "PASS" : "FAIL", text.substring(0, Math.min(70, text.length())));
        }
        ok &= n == 6;
        System.out.println(ok ? "R14_COMMITTEE_PARITY_PASS programs=" + n : "R14_COMMITTEE_PARITY_FAIL");
        if (!ok) System.exit(1);
    }
}
