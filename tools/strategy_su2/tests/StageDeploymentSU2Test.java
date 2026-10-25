package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Production P49 training, immutable identity, repeat capture and checkpoint continuation on unseen periods. */
public final class StageDeploymentSU2Test {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static ModelGenomeSU2 program(String kind) {
        Map<String, String> p = new TreeMap<>();
        for (Map.Entry<String, String[]> e : ModelEvolutionSU2.GRID.get(kind).entrySet()) p.put(e.getKey(), e.getValue()[e.getValue().length / 2]);
        return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind, p, 1)), "STATIC", false, "MASS", .30, .2, true);
    }
    public static void main(String[] args) throws Exception {
        int[][] all = ModelEvolutionR14Test.load(Paths.get(args[0], "REGIME_seed1.txt"));
        int N = 3400; int[][] train = Arrays.copyOf(all, N);
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(train, 96L << 20);
        ModelEvolutionSU2.State state = new ModelEvolutionSU2.State();
        List<String> texts = new ArrayList<>(); int i = 0;
        double[][] conf = new double[3][]; boolean[][] hit = new boolean[3][];
        for (String kind : new String[]{"MSDIR", "REGMEM", "RELBAYES"}) {
            ModelGenomeSU2 g = program(kind); CommitteeSU2 c = new CommitteeSU2(Collections.singletonList(g), new double[]{.15, .30, 1}[i]);
            state.committees.put(ModelEvolutionSU2.TIERS[i], c); texts.add(c.text());
            ModelReplaySU2.Result r = CommitteeSU2.evaluate(c, lib, 1500); conf[i] = r.phat; hit[i] = r.hit; i++;
        }
        List<StageStrategySU2.Definition> accepted = StageStrategySU2.prepare(state, lib, N - 1);
        for (StageStrategySU2.Definition d : accepted) {
            require(d.validationActs >= 30 && d.baseActs > 0, "insufficient validation");
            require(d.validationHits / (double) d.validationActs > d.baseHits / (double) d.baseActs, "validation safeguard bypassed");
            require(d.candidates.stream().anyMatch(s -> s.contains("CONTMAT")), "continuation candidate missing");
        }
        float[][] rs = lib.get(StageStrategySU2.REGIME).state; double[] st0 = new double[N]; for (int t = 0; t < N; t++) st0[t] = rs[t][0];
        double[][] F = StageLayerSU2.fingerprints(train, hit, st0);
        StageLayerSU2.Layer layer = StageLayerSU2.fit(F, conf, hit, 1500, N);
        double[] seed = StageStrategySU2.maxima(StageLayerSU2.apply(layer, F, conf, N - 720, N));
        StageStrategySU2.Definition def = new StageStrategySU2.Definition("F15", .15, N - 1, texts, layer, seed);
        byte[] bundle = def.bytes(); StageStrategySU2.Definition decoded = StageStrategySU2.Definition.read(bundle);
        require(Arrays.equals(bundle, decoded.bytes()) && def.key().equals(decoded.key()), "bundle identity changed on restart");
        byte[] tampered = bundle.clone(); tampered[tampered.length - 1] ^= 1;
        require(!def.text().equals(StageStrategySU2.Definition.read(tampered).text()), "trained parameter not bound to identity");
        ModelLiveSU2.Hub hub = new ModelLiveSU2.Hub();
        StageStrategySU2 live = new StageStrategySU2(def, hub, 0, 1500, 2500, 250);
        for (int t = 0; t < N + 10; t++) {
            ModelLiveSU2.Decision d = live.decide(); require(live.decide() == d, "repeat capture advanced stage filter");
            if (t < N) require(!d.act, "training history used as live evidence");
            live.settle(all[t]); hub.update(all[t]);
        }
        ByteArrayOutputStream hb = new ByteArrayOutputStream(), sb = new ByteArrayOutputStream();
        hub.write(new DataOutputStream(hb)); live.write(new DataOutputStream(sb));
        ModelLiveSU2.Hub restoredHub = new ModelLiveSU2.Hub(); StageStrategySU2 restored = new StageStrategySU2(decoded, restoredHub, 0, 1500, 2500, 250);
        restoredHub.readInto(new DataInputStream(new ByteArrayInputStream(hb.toByteArray())));
        restored.readInto(new DataInputStream(new ByteArrayInputStream(sb.toByteArray())));
        for (int t = N + 10; t < N + 60; t++) {
            ModelLiveSU2.Decision a = live.decide(), b = restored.decide();
            require(a.mask == b.mask && a.position == b.position && a.act == b.act && a.phat == b.phat, "stage checkpoint diverged at " + t);
            require(live.pendingTable(t) != null && restored.pendingTable(t) != null, "mainline action table missing");
            require(live.settle(all[t]) == restored.settle(all[t]), "settlement diverged"); hub.update(all[t]); restoredHub.update(all[t]);
        }
        require(def.bytes().length <= StageStrategySU2.MAX_BUNDLE_BYTES && def.calibrators().size() <= 32, "stage budget exceeded");
        System.out.println("P49_STAGE_DEPLOYMENT_PASS: validation guard, causal deployment, bound identity, capture retry and restart parity; accepted=" + accepted.size());
    }
}
