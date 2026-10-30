package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/** R27-033/034 AtomNet training (docs/06 6.3). Historical pre-training on H_TRAIN (Adam lr 1e-3, batch 64, cosine
 * decay to 1e-5, ≤ 20 epochs, early stop on H_SELECT log-loss with patience 3), holdout check, then continual online
 * training: one mini-batch per settled period = the new sample + 63 replayed from the last 2,000, at
 * (end-of-pretraining lr)×0.1. Samples come from draws by the PIT rule (AtomReplayR27), not from pre-draw freezes,
 * so a late freeze can no longer stop training. Every 720 periods the newest holdout block is scored; a log-loss
 * worse than the previous check by more than 2 standard errors rolls back to the previous checkpoint and halves the
 * online learning rate (ROLLBACK_DEGRADED). */
public final class AtomNetTrainerR27 {
    public static final int BATCH = 64, MAX_EPOCHS = 20, PATIENCE = 3, REPLAY = 2000, CHECK_EVERY = 720;
    public static final double LR0 = 1e-3, LR_MIN = 1e-5;

    public static final class Sample { public final double[] x; public final int y, cls; public final String issue; public Sample(double[] x, int y, int cls, String issue) { this.x = x; this.y = y; this.cls = cls; this.issue = issue; } }
    public static final class Epoch { public final int epoch; public final String split; public final double logloss, brier; Epoch(int e, String s, double l, double b) { epoch = e; split = s; logloss = l; brier = b; } }

    public AtomNetR27 net; public double onlineLr; public long onlineSteps; public final List<Epoch> epochs = new ArrayList<>();
    public byte[] checkpoint; public double lastCheckLogloss = Double.NaN, lastCheckSe = Double.NaN; public long sinceCheck; public final List<String> events = new ArrayList<>();
    int nextEpochReceipt;
    // Package access for the versioned full-state codec, not just a weights checkpoint.
    final List<Sample> buffer = new ArrayList<>(); final StatsR27.SplitMix rng;

    public AtomNetTrainerR27(int in, long seed) { net = new AtomNetR27(in, seed); rng = new StatsR27.SplitMix(seed ^ 0x5EEDL); }

    /** @return epochs run. */
    public int pretrain(List<Sample> train, List<Sample> select) {
        if (train.isEmpty()) return 0;
        double best = Double.POSITIVE_INFINITY; int bad = 0, e; byte[] bestNet = net.encode(); double lr = LR0;
        int stepsPerEpoch = Math.max(1, (train.size() + BATCH - 1) / BATCH), total = MAX_EPOCHS * stepsPerEpoch, step = 0;
        for (e = 0; e < MAX_EPOCHS; e++) {
            CooperativeContinuationR26.poll();
            List<Integer> order = new ArrayList<>(); for (int i = 0; i < train.size(); i++) order.add(i);
            for (int i = order.size() - 1; i > 0; i--) { int j = rng.nextInt(i + 1); Integer x = order.get(i); order.set(i, order.get(j)); order.set(j, x); }
            for (int s = 0; s < order.size(); s += BATCH) {
                CooperativeContinuationR26.poll();
                lr = LR_MIN + 0.5 * (LR0 - LR_MIN) * (1 + Math.cos(Math.PI * step / (double) total)); step++;
                int to = Math.min(order.size(), s + BATCH); double[][] xs = new double[to - s][]; int[] y = new int[to - s], c = new int[to - s];
                for (int k = s; k < to; k++) { Sample q = train.get(order.get(k)); xs[k - s] = q.x; y[k - s] = q.y; c[k - s] = q.cls; }
                net.trainBatch(xs, y, c, lr, rng);
            }
            double[] tr = evaluate(net, train), se = evaluate(net, select);
            epochs.add(new Epoch(e, SplitR27.TRAIN, tr[0], tr[1])); epochs.add(new Epoch(e, SplitR27.SELECT, se[0], se[1]));
            if (se[0] < best - 1e-6) { best = se[0]; bad = 0; bestNet = net.encode(); } else if (++bad >= PATIENCE) { e++; break; }
        }
        net = AtomNetR27.decode(bestNet); onlineLr = lr * 0.1; checkpoint = net.encode();
        buffer.clear(); buffer.addAll(train.subList(Math.max(0, train.size() - REPLAY), train.size()));
        for (Sample s : select) remember(s);
        events.add("PRETRAINED epochs=" + e + " bestSelectLogloss=" + best + " onlineLr=" + onlineLr);
        nextEpochReceipt = epochs.size();
        return e;
    }

    /** {log-loss of the hit head, Brier of the hit head, standard error of log-loss}. */
    public static double[] evaluate(AtomNetR27 net, List<Sample> xs) {
        if (xs.isEmpty()) return new double[] {Double.NaN, Double.NaN, Double.NaN};
        StatsR27.Moments ll = new StatsR27.Moments(); double br = 0;
        for (Sample s : xs) { CooperativeContinuationR26.poll();double p = net.predict(s.x); ll.add(-(s.y == 1 ? Math.log(Math.max(1e-12, p)) : Math.log(Math.max(1e-12, 1 - p)))); br += (p - s.y) * (p - s.y); }
        return new double[] {ll.mean, br / xs.size(), ll.se()};
    }

    void remember(Sample s) { buffer.add(s); if (buffer.size() > REPLAY) buffer.remove(0); }

    /** One online step for a newly settled sample (experience replay). Returns the batch loss. */
    public double online(Sample s) {
        int extra = Math.min(BATCH - 1, buffer.size());
        double[][] xs = new double[extra + 1][]; int[] y = new int[extra + 1], c = new int[extra + 1];
        xs[0] = s.x; y[0] = s.y; c[0] = s.cls;
        for (int k = 1; k <= extra; k++) { Sample q = buffer.get(rng.nextInt(buffer.size())); xs[k] = q.x; y[k] = q.y; c[k] = q.cls; }
        double loss = net.trainBatch(xs, y, c, onlineLr, rng);
        remember(s); onlineSteps++; sinceCheck++;
        return loss;
    }

    /** Periodic holdout check with rollback (docs/06 6.3-5). Returns true when a rollback happened. */
    public boolean check(List<Sample> holdoutBlock) {
        double[] h = evaluate(net, holdoutBlock); sinceCheck = 0;
        boolean degrade = Double.isFinite(lastCheckLogloss) && h[0] > lastCheckLogloss + 2 * Math.max(h[2], lastCheckSe);
        if (degrade) { net = AtomNetR27.decode(checkpoint); onlineLr *= 0.5; events.add("ROLLBACK_DEGRADED logloss=" + h[0] + " previous=" + lastCheckLogloss); return true; }
        lastCheckLogloss = h[0]; lastCheckSe = h[2]; checkpoint = net.encode(); epochs.add(new Epoch(nextEpochReceipt++, SplitR27.HOLDOUT, h[0], h[1]));
        return false;
    }
}
