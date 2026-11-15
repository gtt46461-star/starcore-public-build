package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Run with main from the package root; checks Python-to-Java golden parity and uniformity. */
public final class NullBaselineTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static void rejects(Runnable r, String message) {
        checks++;
        try { r.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("not rejected: " + message);
    }

    public static void main(String[] args) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("reference/fixtures/null_baseline_golden.json")), StandardCharsets.UTF_8);
        Matcher seedM = Pattern.compile("\"seed\":\\s*\"([0-9a-f]{64})\"").matcher(json);
        check(seedM.find(), "golden seed present");
        String seed = seedM.group(1);
        Pattern row = Pattern.compile("\"issue\":\\s*(\\d+),\\s*\"position\":\\s*(\\d+),\\s*\"numbers\":\\s*\\[\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\s*\\]");
        Matcher m = row.matcher(json);
        int vectors = 0;
        while (m.find()) {
            NullBaseline.Choice c = NullBaseline.choose(seed, Long.parseLong(m.group(1)));
            int[] n = c.numbers();
            check(c.position == Integer.parseInt(m.group(2)), "position parity issue " + m.group(1));
            check(n[0] == Integer.parseInt(m.group(3)) && n[1] == Integer.parseInt(m.group(4))
                    && n[2] == Integer.parseInt(m.group(5)), "numbers parity issue " + m.group(1));
            vectors++;
        }
        check(vectors >= 20, "golden vector count " + vectors);

        rejects(() -> NullBaseline.choose("ABC", 1), "bad seed");
        rejects(() -> NullBaseline.choose(seed, 0), "non-positive issue");

        int n = 20000, hits = 0;
        int[] pos = new int[11];
        int[] identity = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        boolean canonical = true;
        for (int issue = 1; issue <= n; issue++) {
            NullBaseline.Choice c = NullBaseline.choose(seed, issue);
            pos[c.position]++;
            int[] x = c.numbers();
            canonical &= x[0] < x[1] && x[1] < x[2] && x[0] >= 1 && x[2] <= 10;
            if (c.hits(identity)) hits++;
        }
        check(canonical, "every choice has ascending distinct numbers in 1..10");
        for (int k = 1; k <= 10; k++) check(Math.abs(pos[k] / (double) n - 0.1) < 0.012, "uniform position " + k);
        check(Math.abs(hits / (double) n - 0.3) < 0.015, "exact 30% hit on a maximally biased game: " + hits / (double) n);
        check(!NullBaseline.mayAuthorizeReal(), "comparator never authorizes");
        System.out.println("NullBaselineTest PASS checks=" + checks + " scope=PURE_JAVA_REFERENCE_NOT_ANDROID_NOT_REAL_PERFORMANCE");
    }
}
