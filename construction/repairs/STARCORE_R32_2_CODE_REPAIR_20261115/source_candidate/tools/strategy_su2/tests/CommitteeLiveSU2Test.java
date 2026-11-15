package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** R14 live == replay: calibrated programs and committees decided/settled one period at a time reproduce the Python
 *  reference exactly; a checkpoint at t=5000 restored into fresh objects continues identically; a committee born at
 *  t=6000 bootstraps from history and then matches too. */
public final class CommitteeLiveSU2Test {
    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[0]);
        List<String> lines = Files.readAllLines(dir.resolve("REGIME9000_draws.txt")); int T = lines.size(); int[][] d = new int[T][10];
        for (int t = 0; t < T; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) d[t][p] = Integer.parseInt(f[p]); }
        String json = new String(Files.readAllBytes(dir.resolve("programs.json")), "UTF-8");
        Matcher m = Pattern.compile("\"name\": \"(\\w+)\",\\s*\"text\": \"([^\"]+)\",\\s*\"act\": \"([01]+)\",\\s*\"hit\": \"([01]+)\",\\s*\"position\": \"(\\d+)\"").matcher(json);
        List<String> names = new ArrayList<>(), texts = new ArrayList<>(), acts = new ArrayList<>(), hits = new ArrayList<>(), poss = new ArrayList<>();
        while (m.find()) { names.add(m.group(1)); texts.add(m.group(2)); acts.add(m.group(3)); hits.add(m.group(4)); poss.add(m.group(5)); }
        int N = names.size(); int[] mism = new int[N];
        ModelLiveSU2.Hub hub = new ModelLiveSU2.Hub();
        LiveStrategySU2[] live = new LiveStrategySU2[N];
        for (int i = 0; i < N; i++) live[i] = make(texts.get(i), hub);
        for (int t = 0; t < T; t++) {
            if (t == 5000) {                                                        // checkpoint -> fresh objects -> restore
                ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b);
                hub.write(o); for (LiveStrategySU2 l : live) l.write(o); o.flush();
                ModelLiveSU2.Hub h2 = new ModelLiveSU2.Hub(); LiveStrategySU2[] l2 = new LiveStrategySU2[N];
                for (int i = 0; i < N; i++) l2[i] = make(texts.get(i), h2);
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(b.toByteArray()));
                h2.readInto(in); for (LiveStrategySU2 l : l2) l.readInto(in);
                hub = h2; live = l2;
                System.out.println("checkpoint bytes at t=5000: " + b.size());
            }
            for (int i = 0; i < N; i++) {
                ModelLiveSU2.Decision x = live[i].decide();
                boolean hit = live[i].settle(d[t]);
                if (x.act != (acts.get(i).charAt(t) == '1') || hit != (hits.get(i).charAt(t) == '1') || x.position - 1 != poss.get(i).charAt(t) - '0') mism[i]++;
            }
            hub.update(d[t]);
        }
        boolean ok = true;
        for (int i = 0; i < N; i++) { System.out.println("live==replay " + names.get(i) + " mismatches=" + mism[i] + (mism[i] == 0 ? " PASS" : " FAIL")); ok &= mism[i] == 0; }
        // late birth: committee c1 joins at t=6000 by replaying the stored history through a private hub
        int ci = names.indexOf("c1"); ModelLiveSU2.Hub shared = new ModelLiveSU2.Hub(); LiveStrategySU2 g1 = make(texts.get(names.indexOf("g1")), shared);
        CommitteeLiveSU2 late = null; int lm = 0;
        for (int t = 0; t < T; t++) {
            if (t == 6000) late = CommitteeLiveSU2.bootstrap(CommitteeSU2.parse(texts.get(ci)), shared, Arrays.copyOf(d, 6000), 1500, 2500, 250);
            g1.decide(); g1.settle(d[t]);
            if (late != null) { ModelLiveSU2.Decision x = late.decide(); boolean hit = late.settle(d[t]); if (x.act != (acts.get(ci).charAt(t) == '1') || hit != (hits.get(ci).charAt(t) == '1')) lm++; }
            shared.update(d[t]);
        }
        System.out.println("late-born committee bootstrap mismatches=" + lm + (lm == 0 ? " PASS" : " FAIL")); ok &= lm == 0;
        System.out.println(ok ? "R14_LIVE_EQUALS_REPLAY_PASS" : "R14_LIVE_EQUALS_REPLAY_FAIL");
        if (!ok) System.exit(1);
    }

    static LiveStrategySU2 make(String text, ModelLiveSU2.Hub hub) {
        return CommitteeSU2.isCommittee(text) ? new CommitteeLiveSU2(CommitteeSU2.parse(text), hub, 1500, 2500, 250)
            : new ModelLiveSU2(ModelGenomeSU2.parse(text), hub, 1500, 2500, 250);
    }
}
