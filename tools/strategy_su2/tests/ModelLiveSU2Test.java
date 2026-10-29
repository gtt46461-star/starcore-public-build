package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Live incremental execution == historical replay, including save/restore and late-born strategies. */
public final class ModelLiveSU2Test {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]);
        List<String> lines = Files.readAllLines(dir.resolve("draws.txt"));
        int[][] d = new int[lines.size()][10];
        for (int t = 0; t < d.length; t++) { String[] f = lines.get(t).trim().split("\\s+"); for (int p = 0; p < 10; p++) d[t][p] = Integer.parseInt(f[p]); }
        String json = new String(Files.readAllBytes(dir.resolve("genomes.json")), "UTF-8");
        Matcher m = Pattern.compile("\\{\"text\": \"([^\"]+)\"").matcher(json);
        List<ModelGenomeSU2> gs = new ArrayList<>();
        while (m.find()) gs.add(ModelGenomeSU2.parse(m.group(1)));
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 512L << 20);
        boolean ok = true; int T = d.length;
        // 1) all genomes from genesis on one shared hub, with a full save/restore at t=3000
        ModelLiveSU2.Hub hub = new ModelLiveSU2.Hub();
        List<ModelLiveSU2> lives = new ArrayList<>();
        for (ModelGenomeSU2 g : gs) lives.add(new ModelLiveSU2(g, hub, 1500, 2500, 250));
        int[] mism = new int[gs.size()];
        List<ModelReplaySU2.Result> ref = new ArrayList<>();
        for (ModelGenomeSU2 g : gs) ref.add(ModelReplaySU2.evaluate(g, lib, 1500, 2500, 250));
        for (int t = 0; t < T; t++) {
            if (t == 3000) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
                hub.write(out); for (ModelLiveSU2 l : lives) l.write(out); out.flush();
                ModelLiveSU2.Hub fresh = new ModelLiveSU2.Hub(); List<ModelLiveSU2> again = new ArrayList<>();
                for (ModelGenomeSU2 g : gs) again.add(new ModelLiveSU2(g, fresh, 1500, 2500, 250));
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
                fresh.readInto(in); for (ModelLiveSU2 l : again) l.readInto(in);
                hub = fresh; lives = again;
                System.out.println("state bytes at t=3000: " + bytes.size());
            }
            for (int i = 0; i < gs.size(); i++) {
                ModelLiveSU2.Decision x = lives.get(i).decide();
                ModelReplaySU2.Result r = ref.get(i);
                if (x.act != r.act[t] || x.position != r.position[t] || x.mask != r.mask[t]) mism[i]++;
            }
            for (int i = 0; i < gs.size(); i++) if (lives.get(i).settle(d[t]) != ref.get(i).hit[t]) mism[i]++;
            hub.update(d[t]);
        }
        for (int i = 0; i < gs.size(); i++) { boolean pass = gs.get(i).pool.equals("GATED") ? mism[i] <= d.length / 100 : mism[i] == 0; ok &= pass; System.out.println("live==replay " + gs.get(i).text().substring(0, 60) + " mismatches=" + mism[i] + (pass ? " PASS" : " FAIL")); }
        // 2) a strategy born at t=4000 bootstraps from history and continues identically
        ModelGenomeSU2 late = gs.get(gs.size() - 1);
        ModelLiveSU2.Hub hub2 = new ModelLiveSU2.Hub();
        for (int t = 0; t < 4000; t++) { hub2.predict(); hub2.update(d[t]); }
        ModelLiveSU2 born = ModelLiveSU2.bootstrap(late, hub2, Arrays.copyOf(d, 4000), 1500, 2500, 250);
        int lm = 0; ModelReplaySU2.Result r = ref.get(gs.size() - 1);
        for (int t = 4000; t < T; t++) { ModelLiveSU2.Decision x = born.decide(); if (x.act != r.act[t] || x.mask != r.mask[t]) lm++; if (born.settle(d[t]) != r.hit[t]) lm++; hub2.update(d[t]); }
        ok &= lm == 0; System.out.println("late-born bootstrap mismatches=" + lm + (lm == 0 ? " PASS" : " FAIL"));
        System.out.println(ok ? "R13_LIVE_EQUALS_REPLAY_PASS" : "R13_LIVE_EQUALS_REPLAY_FAIL");
        if (!ok) System.exit(1);
    }
}
