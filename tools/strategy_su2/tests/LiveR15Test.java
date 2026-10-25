package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * R15 strategies run live (one period at a time, shared hub, calibrators, teacher-matrix pool, molecules, both gates)
 * produce exactly the replay decisions, including a save/restore of the whole live state at t=3000 and a committee
 * built from them. Draws: fixture r14b_atomnet (5,000 periods). Prints R15_LIVE_EQUALS_REPLAY_PASS.
 */
public final class LiveR15Test {
    public static void main(String[] args) throws Exception {
        Path d14 = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r14b_atomnet"), d15 = Paths.get(args.length > 1 ? args[1] : "reference/fixtures/r15_atomnet");
        int[][] d = AtomNetSU2Test.draws(d14.resolve("draws.txt")); int T = d.length;
        ModelGenomeSU2.Member xi = new ModelGenomeSU2.Member("ATOMNET", AtomNetSU2Test.member(Files.readAllLines(d15.resolve("cfg4.txt")).get(0)), 1.0);
        ModelGenomeSU2.Member mol = new ModelGenomeSU2.Member("ATOMNET", AtomNetSU2Test.member(Files.readAllLines(d15.resolve("cfg5.txt")).get(0)), 1.0);
        ModelGenomeSU2.Member hmm = new ModelGenomeSU2.Member("HMM", ModelEvolutionSU2.centralParams("HMM"), 1.0);
        ModelGenomeSU2.Member msd = new ModelGenomeSU2.Member("MSDIR", ModelEvolutionSU2.centralParams("MSDIR"), 1.0);
        int jEnt = -1, jReg = -1;
        for (int j = 0; j < AtomStreamSU2.states(); j++) { String n = AtomStreamSU2.STATES[j].name(); if (n.equals("ENTROPY(window=50)")) jEnt = j; if (n.equals("REGIME_SCORE(regime=LOW_ENTROPY,window=120)")) jReg = j; }
        List<ModelGenomeSU2> gs = Arrays.asList(
            new ModelGenomeSU2(Arrays.asList(mol, msd), "TEACHER:" + jEnt, false, "MASS", 0.15, 0.2, true),
            new ModelGenomeSU2(Arrays.asList(xi, hmm), "TEACHER:" + jReg, false, "META", 0.30, 0.2, true),
            new ModelGenomeSU2(Collections.singletonList(xi), "STATIC", false, "MASS", 0.15, 0.2, true));
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 512L << 20);
        List<ModelReplaySU2.Result> ref = new ArrayList<>(); for (ModelGenomeSU2 g : gs) ref.add(ModelReplaySU2.evaluate(g, lib, 1500, 2500, 250));
        CommitteeSU2 com = new CommitteeSU2(Arrays.asList(gs.get(0).asTier(0.15), gs.get(2).asTier(0.15)), 0.15);
        ModelReplaySU2.Result cref = CommitteeSU2.evaluate(com, lib, 1500);
        ModelLiveSU2.Hub hub = new ModelLiveSU2.Hub();
        List<ModelLiveSU2> lives = new ArrayList<>(); for (ModelGenomeSU2 g : gs) lives.add(new ModelLiveSU2(g, hub, 1500, 2500, 250));
        CommitteeLiveSU2 clive = new CommitteeLiveSU2(com, hub, 1500, 2500, 250);
        int[] mism = new int[gs.size() + 1]; long t0 = System.nanoTime();
        for (int t = 0; t < T; t++) {
            if (t == 3000) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
                hub.write(out); for (ModelLiveSU2 l : lives) l.write(out); clive.write(out); out.flush();
                ModelLiveSU2.Hub fresh = new ModelLiveSU2.Hub(); List<ModelLiveSU2> again = new ArrayList<>();
                for (ModelGenomeSU2 g : gs) again.add(new ModelLiveSU2(g, fresh, 1500, 2500, 250));
                CommitteeLiveSU2 c2 = new CommitteeLiveSU2(com, fresh, 1500, 2500, 250);
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
                fresh.readInto(in); for (ModelLiveSU2 l : again) l.readInto(in); c2.readInto(in);
                hub = fresh; lives = again; clive = c2;
                System.out.println("live state bytes at t=3000: " + bytes.size());
            }
            for (int i = 0; i < gs.size(); i++) { ModelLiveSU2.Decision x = lives.get(i).decide(); ModelReplaySU2.Result r = ref.get(i); if (x.act != r.act[t] || x.position != r.position[t] || x.mask != r.mask[t]) mism[i]++; }
            ModelLiveSU2.Decision cx = clive.decide(); if (cx.act != cref.act[t] || cx.position != cref.position[t] || cx.mask != cref.mask[t]) mism[gs.size()]++;
            for (int i = 0; i < gs.size(); i++) if (lives.get(i).settle(d[t]) != ref.get(i).hit[t]) mism[i]++;
            if (clive.settle(d[t]) != cref.hit[t]) mism[gs.size()]++;
            hub.update(d[t]);
        }
        boolean ok = true;
        for (int i = 0; i <= gs.size(); i++) {
            String name = i < gs.size() ? gs.get(i).text() : com.text();
            ok &= mism[i] == 0; System.out.println("live==replay " + name.substring(0, Math.min(110, name.length())) + "… mismatches=" + mism[i]);
        }
        System.out.printf(Locale.ROOT, "%.2f ms/period for %d strategies + 1 committee%n", (System.nanoTime() - t0) / 1e6 / T, gs.size());
        System.out.println(ok ? "R15_LIVE_EQUALS_REPLAY_PASS" : "R15_LIVE_EQUALS_REPLAY_FAIL");
        if (!ok) System.exit(1);
    }
}
