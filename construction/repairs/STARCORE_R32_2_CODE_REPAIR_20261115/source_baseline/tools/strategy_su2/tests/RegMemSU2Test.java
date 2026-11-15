package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * R16 (P48) regime-memory families against the Python reference (fixture r16_regmem, 5,000 REGIME draws):
 *  1. five REGMEM / RELMEM configurations: canonical member text == Python, probability table and state == Python;
 *  2. checkpoint continuity: write the model at t=2500, read into a fresh instance, continue == uninterrupted run;
 *  3. live == replay: calibrated strategies with REGMEM / RELMEM members (STATIC, GATED, both gates) and a committee
 *     run one period at a time on a shared hub with a whole-state save/restore at t=3000 give the replay decisions.
 * Prints R16_REGMEM_PARITY_PASS.
 */
public final class RegMemSU2Test {
    static ModelGenomeSU2.Member member(String text) {
        return ModelGenomeSU2.parse("SU2|POOL=STATIC|SINK=0|GATE=MASS|COV=0.30|M=" + text.trim()).members.get(0);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "reference/fixtures/r16_regmem");
        int[][] D = AtomNetSU2Test.draws(dir.resolve("draws.txt")); int T = D.length;
        boolean ok = true; List<ModelGenomeSU2.Member> ms = new ArrayList<>();
        for (String name : new String[]{"regmem3", "regmem2", "relmem_rel", "relmem_mix", "relmem_src"}) {
            String text = Files.readAllLines(dir.resolve(name + ".txt")).get(0).trim();
            ModelGenomeSU2.Member m = member(text); ms.add(m);
            boolean textOk = (m.modelKey() + "*1").equals(text);
            float[] PP = AtomNetSU2Test.f32(dir.resolve(name + ".P.f32")), SS = AtomNetSU2Test.f32(dir.resolve(name + ".ST.f32"));
            OnlineModelsSU2.Model x = ModelReplaySU2.build(m.kind, m.params), y = ModelReplaySU2.build(m.kind, m.params);
            float[][] p = new float[10][10], q = new float[10][10]; float[] s = new float[3], s2 = new float[3];
            double wp = 0, ws = 0; int cont = 0; long t0 = System.nanoTime();
            for (int t = 0; t < T; t++) {
                x.predict(p, s); y.predict(q, s2);
                for (int a = 0; a < 10; a++) for (int n = 0; n < 10; n++) { wp = Math.max(wp, Math.abs(p[a][n] - PP[(t * 10 + a) * 10 + n])); if (p[a][n] != q[a][n]) cont++; }
                for (int k = 0; k < 3; k++) ws = Math.max(ws, Math.abs(s[k] - SS[t * 3 + k]));
                x.update(D[t]); y.update(D[t]);
                if (t == 2499) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream(); y.write(new DataOutputStream(bo));
                    OnlineModelsSU2.Model z = ModelReplaySU2.build(m.kind, m.params); z.read(new DataInputStream(new ByteArrayInputStream(bo.toByteArray()))); y = z;
                }
            }
            boolean pass = textOk && wp < 2e-5 && ws < 1e-4 && cont == 0; ok &= pass;
            System.out.printf(Locale.ROOT, "%-11s %s text %s | max|dP| %.2e max|dST| %.2e | checkpoint continuity diffs %d | %.3f ms/period | %s%n",
                name, m.modelKey(), textOk ? "==" : "!=", wp, ws, cont, (System.nanoTime() - t0) / 1e6 / T / 2, pass ? "ok" : "FAIL");
        }
        // 3. live == replay for strategies built from the new families
        ModelGenomeSU2.Member hmm = new ModelGenomeSU2.Member("HMM", ModelEvolutionSU2.centralParams("HMM"), 1.0);
        List<ModelGenomeSU2> gs = Arrays.asList(
            new ModelGenomeSU2(Collections.singletonList(ms.get(0)), "STATIC", false, "MASS", 0.15, 0.2, true),
            new ModelGenomeSU2(Arrays.asList(ms.get(3), hmm), "GATED", false, "META", 0.30, 0.2, true),
            new ModelGenomeSU2(Arrays.asList(ms.get(2), ms.get(4)), "BMA", true, "MASS", 0.30, 0.2, true));
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(D, 512L << 20);
        List<ModelReplaySU2.Result> ref = new ArrayList<>(); for (ModelGenomeSU2 g : gs) ref.add(ModelReplaySU2.evaluate(g, lib, 1500, 2500, 250));
        CommitteeSU2 com = new CommitteeSU2(Arrays.asList(gs.get(0).asTier(0.15), gs.get(2).asTier(0.15)), 0.15);
        ModelReplaySU2.Result cref = CommitteeSU2.evaluate(com, lib, 1500);
        ModelLiveSU2.Hub hub = new ModelLiveSU2.Hub();
        List<ModelLiveSU2> lives = new ArrayList<>(); for (ModelGenomeSU2 g : gs) lives.add(new ModelLiveSU2(g, hub, 1500, 2500, 250));
        CommitteeLiveSU2 clive = new CommitteeLiveSU2(com, hub, 1500, 2500, 250);
        int[] mism = new int[gs.size() + 1]; int acts = 0;
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
            for (int i = 0; i < gs.size(); i++) { ModelLiveSU2.Decision x = lives.get(i).decide(); ModelReplaySU2.Result r = ref.get(i); if (x.act != r.act[t] || x.position != r.position[t] || x.mask != r.mask[t]) mism[i]++; if (r.act[t]) acts++; }
            ModelLiveSU2.Decision cx = clive.decide(); if (cx.act != cref.act[t] || cx.position != cref.position[t] || cx.mask != cref.mask[t]) mism[gs.size()]++;
            for (int i = 0; i < gs.size(); i++) if (lives.get(i).settle(D[t]) != ref.get(i).hit[t]) mism[i]++;
            if (clive.settle(D[t]) != cref.hit[t]) mism[gs.size()]++;
            hub.update(D[t]);
        }
        for (int i = 0; i <= gs.size(); i++) {
            String name = i < gs.size() ? gs.get(i).text() : com.text();
            ok &= mism[i] == 0; System.out.println("live==replay " + name.substring(0, Math.min(120, name.length())) + "… mismatches=" + mism[i]);
        }
        System.out.println("replay actions checked: " + acts);
        System.out.println(ok ? "R16_REGMEM_PARITY_PASS" : "R16_REGMEM_PARITY_FAIL");
        if (!ok) System.exit(1);
    }
}
