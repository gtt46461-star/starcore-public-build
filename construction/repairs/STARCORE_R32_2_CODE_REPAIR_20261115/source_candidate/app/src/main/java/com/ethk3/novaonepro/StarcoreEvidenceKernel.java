package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * B07 merged evidence kernel. It owns immutable evidence packets, plugin ordering, timeout
 * isolation, evidence-group de-correlation and replay health only. It MUST NOT create
 * Decision/Trade/BET and MUST NOT write FinalP, Lane or gate.
 */
public final class StarcoreEvidenceKernel {
    public static final String VERSION = AppConfig.B07R3_VERSION;
    public static final String STATE_LAST = "starcore_kernel_last";
    public static final String STATE_REPLAY = "starcore_kernel_replay";
    public static final String STATE_HEALTH = "starcore_kernel_health";
    public static final String STATE_CALLS = "starcore_kernel_calls";
    public static final String STATE_TIMEOUTS = "starcore_kernel_timeouts";
    private static final List<Plugin> REGISTRY = buildRegistry();
    private static final ThreadLocal<Map<String, String>> R923_HEALTH_SNAPSHOT =
            new ThreadLocal<Map<String, String>>();

    /**
     * Installs one detached plugin-health snapshot for the current prediction thread.
     *
     * <p>There are 64 probes and each used to issue two independent SQLite reads.  A caller can
     * now preload the whole namespace with one primary-key range query, then even invoke the
     * immutable kernel with a null store without losing replay-health evidence.</p>
     */
    public static HealthReadScope openHealthReadScopeR923(DbStore store) {
        Map<String, String> previous = R923_HEALTH_SNAPSHOT.get();
        Map<String, String> snapshot = store == null
                ? Collections.<String, String>emptyMap()
                : store.readStatePrefixR923("starcore_kernel_plugin_", 256);
        R923_HEALTH_SNAPSHOT.set(snapshot);
        return new HealthReadScope(previous);
    }

    public static final class HealthReadScope implements AutoCloseable {
        private final Map<String, String> previous;
        private boolean closed;

        HealthReadScope(Map<String, String> previousSnapshot) {
            previous = previousSnapshot;
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) R923_HEALTH_SNAPSHOT.remove();
            else R923_HEALTH_SNAPSHOT.set(previous);
        }
    }

    public static final class EvidencePacket {
        public final String pluginId;
        public final String group;
        public final double support;
        public final double counter;
        public final double confidence;
        public final int sample;
        public final long elapsedNs;
        public final boolean timedOut;
        public final String health;
        public final String fingerprint;

        EvidencePacket(
                String id,
                String g,
                double s,
                double r,
                double c,
                int n,
                long ns,
                boolean to,
                String h,
                String fp) {
            pluginId = safe(id);
            group = safe(g);
            support = clamp01(s);
            counter = clamp01(r);
            confidence = clamp01(c);
            sample = Math.max(0, n);
            elapsedNs = Math.max(0L, ns);
            timedOut = to;
            health = safe(h);
            fingerprint = safe(fp);
        }

        public String summary() {
            return pluginId
                    + "@"
                    + group
                    + ":S="
                    + fmt(support)
                    + ",R="
                    + fmt(counter)
                    + ",C="
                    + fmt(confidence)
                    + ",N="
                    + sample
                    + ",H="
                    + health
                    + (timedOut ? ",TIMEOUT" : "");
        }
    }

    public static final class HealthSnapshot {
        public final String pluginId;
        public final int sample;
        public final int hit;
        public final double rate;
        public final String state;

        HealthSnapshot(String id, int n, int h, double r, String s) {
            pluginId = safe(id);
            sample = Math.max(0, n);
            hit = Math.max(0, h);
            rate = clamp01(r);
            state = safe(s);
        }

        public String summary() {
            return pluginId + "=" + state + "(" + hit + "/" + sample + "," + fmt(rate) + ")";
        }
    }

    public static final class EvidenceBundle {
        public final ArrayList<EvidencePacket> packets = new ArrayList<EvidencePacket>();
        public final LinkedHashMap<String, Double> groupSupport =
                new LinkedHashMap<String, Double>();
        public final LinkedHashMap<String, Double> groupRisk = new LinkedHashMap<String, Double>();
        public double support;
        public double risk;
        public double confidence;
        public double diversity;
        public int availableGroups;
        public int readyPlugins;
        public int freezePlugins;
        public int timedOutPlugins;
        public int hardRisk;
        public String healthState = "WATCH";
        public String dominantGroup = "";
        public String fingerprint = "";
        public long costMs;

        public String summary() {
            return "Kernel="
                    + healthState
                    + "|groups="
                    + availableGroups
                    + "|support="
                    + fmt(support)
                    + "|risk="
                    + fmt(risk)
                    + "|confidence="
                    + fmt(confidence)
                    + "|diversity="
                    + fmt(diversity)
                    + "|ready="
                    + readyPlugins
                    + "|freeze="
                    + freezePlugins
                    + "|timeouts="
                    + timedOutPlugins
                    + "|hard="
                    + hardRisk
                    + "|dominant="
                    + dominantGroup
                    + "|hash="
                    + shortHash(fingerprint)
                    + "|cost="
                    + costMs
                    + "ms";
        }
    }

    private interface Plugin {
        String id();

        String group();

        EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage);
    }

    public EvidenceBundle evaluate(DbStore store, Models.ComboCandidate c, String stage) {
        return evaluateInternal(store, c, stage, true);
    }

    /** R3.6 pre-draw path: identical evidence reads, no hot-path state writes. */
    public EvidenceBundle evaluateReadOnlyR36(
            DbStore store, Models.ComboCandidate c, String stage) {
        return evaluateInternal(store, c, stage, false);
    }

    private EvidenceBundle evaluateInternal(
            DbStore store, Models.ComboCandidate c, String stage, boolean persist) {
        long begin = System.currentTimeMillis();
        EvidenceBundle out = new EvidenceBundle();
        if (c == null) {
            out.healthState = "WATCH";
            out.fingerprint = hash("NULL|" + safe(stage));
            return out;
        }
        LinkedHashMap<String, Integer> groupCount = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, Double> groupWeightedSupport = new LinkedHashMap<String, Double>();
        LinkedHashMap<String, Double> groupWeightedRisk = new LinkedHashMap<String, Double>();
        LinkedHashMap<String, Double> groupWeight = new LinkedHashMap<String, Double>();
        double totalS = 0.0d, totalR = 0.0d, totalW = 0.0d;
        StringBuilder fp = new StringBuilder(4096);
        for (int i = 0; i < REGISTRY.size(); i++) {
            Plugin p = REGISTRY.get(i);
            EvidencePacket e;
            long t = System.nanoTime();
            try {
                e = p.evaluate(store, c, stage);
            } catch (Throwable x) {
                e =
                        packet(
                                store,
                                p.id(),
                                p.group(),
                                0.0d,
                                0.65d,
                                0.05d,
                                0,
                                System.nanoTime() - t,
                                true,
                                "ERROR");
            }
            long ns = System.nanoTime() - t;
            out.packets.add(e);
            if (e.timedOut) out.timedOutPlugins++;
            if ("FREEZE".equals(e.health)) out.freezePlugins++;
            if ("READY".equals(e.health) || "STRONG".equals(e.health)) out.readyPlugins++;
            Integer old = groupCount.get(e.group);
            int same = old == null ? 0 : old.intValue();
            groupCount.put(e.group, Integer.valueOf(same + 1));
            double decorrelation = 1.0d / (1.0d + same * 0.18d);
            double healthWeight = healthWeight(e.health);
            double w = Math.max(0.02d, e.confidence * decorrelation * healthWeight);
            if (e.timedOut) w *= 0.25d;
            totalS += e.support * w;
            totalR += e.counter * w;
            totalW += w;
            add(groupWeightedSupport, e.group, e.support * w);
            add(groupWeightedRisk, e.group, e.counter * w);
            add(groupWeight, e.group, w);
            fp.append(e.pluginId)
                    .append(":")
                    .append(fmt(e.support))
                    .append(":")
                    .append(fmt(e.counter))
                    .append(":")
                    .append(e.health)
                    .append("|");
        }
        out.support = totalW <= 0.0d ? 0.0d : clamp01(totalS / totalW);
        out.risk = totalW <= 0.0d ? 1.0d : clamp01(totalR / totalW);
        out.confidence = clamp01(totalW / Math.max(1.0d, REGISTRY.size() * 0.55d));
        double best = -1.0d;
        double mean = 0.0d;
        int gc = 0;
        for (Map.Entry<String, Double> x : groupWeight.entrySet()) {
            double w = x.getValue().doubleValue();
            if (w <= 0.0d) continue;
            String g = x.getKey();
            double gs = groupWeightedSupport.get(g).doubleValue() / w;
            double gr = groupWeightedRisk.get(g).doubleValue() / w;
            out.groupSupport.put(g, Double.valueOf(clamp01(gs)));
            out.groupRisk.put(g, Double.valueOf(clamp01(gr)));
            out.availableGroups++;
            mean += gs;
            gc++;
            double dominance = gs - gr;
            if (dominance > best) {
                best = dominance;
                out.dominantGroup = g;
            }
        }
        mean = gc <= 0 ? 0.0d : mean / gc;
        double var = 0.0d;
        for (Double v : out.groupSupport.values()) {
            double d = v.doubleValue() - mean;
            var += d * d;
        }
        var = gc <= 0 ? 1.0d : var / gc;
        out.diversity = clamp01(1.0d - Math.sqrt(var) * 2.0d);
        out.hardRisk =
                (out.freezePlugins >= 3
                                || out.risk >= 0.82d
                                || out.availableGroups < AppConfig.B07R3_KERNEL_MIN_GROUPS)
                        ? 1
                        : 0;
        if (out.hardRisk == 1) out.healthState = "FREEZE";
        else if (out.readyPlugins >= 12 && out.support >= 0.62d && out.risk <= 0.35d)
            out.healthState = "STRONG";
        else if (out.readyPlugins >= 7 && out.support >= 0.52d && out.risk <= 0.48d)
            out.healthState = "READY";
        else out.healthState = "WATCH";
        out.fingerprint = hash(fp.toString() + safe(stage) + "|" + out.healthState);
        out.costMs = System.currentTimeMillis() - begin;
        R3KernelContractIndex.validate(out);
        if (persist) persistBundle(store, out, stage);
        return out;
    }

    public void applyEvidenceOnly(Models.ComboCandidate c, EvidenceBundle b) {
        if (c == null || b == null) return;
        writeDouble(c, "v43StrategySupport", b.support);
        writeDouble(c, "v43StrategyRisk", b.risk);
        writeDouble(c, "v43StrategyConfidence", b.confidence);
        writeDouble(c, "v43StrategyDiversity", b.diversity);
        writeInt(c, "v43EvidenceGroupCount", b.availableGroups);
        writeInt(c, "v43HardRisk", b.hardRisk);
        writeString(c, "v43DominantEvidence", b.dominantGroup);
        writeString(c, "v43EvidenceFingerprint", b.fingerprint);
        writeString(c, "evidenceHealthStatus", b.healthState);
        writeString(c, "ruleHealthStatus", b.healthState);
        writeString(c, "evidenceKernelVersion", VERSION);
        writeString(c, "evidenceKernelSummary", b.summary());
        // Deliberately no writes to finalP, calibratedP, dynamicGate, lane, action or decision.
    }

    public static void observeReplay(
            DbStore store, Models.ComboCandidate c, int actualNumber, String origin) {
        if (store == null || c == null || !c.isValidK3() || actualNumber < 1 || actualNumber > 10)
            return;
        StarcoreEvidenceKernel k = new StarcoreEvidenceKernel();
        EvidenceBundle b = k.evaluate(store, c, "REPLAY:" + safe(origin));
        int hit = (actualNumber == c.n1 || actualNumber == c.n2 || actualNumber == c.n3) ? 1 : 0;
        for (int i = 0; i < b.packets.size(); i++) {
            EvidencePacket e = b.packets.get(i);
            if (e.timedOut) continue;
            String base = healthKey(e.pluginId);
            int n = parseInt(store.getState(base + "_n", "0"), 0);
            int h = parseInt(store.getState(base + "_h", "0"), 0);
            n++;
            h += hit;
            if (n > 720) {
                double q = 720.0d / n;
                n = 720;
                h = (int) Math.round(h * q);
            }
            store.setState(base + "_n", String.valueOf(n));
            store.setState(base + "_h", String.valueOf(h));
            store.setState(base + "_rate", String.valueOf(n <= 0 ? 0.0d : (double) h / n));
        }
        try {
            store.setState(
                    STATE_REPLAY,
                    "origin="
                            + safe(origin)
                            + "|actual="
                            + actualNumber
                            + "|hit="
                            + hit
                            + "|"
                            + b.summary());
        } catch (Throwable ignore) {
        }
    }

    public static String renderHealth(DbStore store) {
        StringBuilder s = new StringBuilder(12000);
        int ready = 0, strong = 0, watch = 0, freeze = 0;
        for (int i = 0; i < REGISTRY.size(); i++) {
            Plugin p = REGISTRY.get(i);
            HealthSnapshot h = health(store, p.id());
            if ("STRONG".equals(h.state)) strong++;
            else if ("READY".equals(h.state)) ready++;
            else if ("FREEZE".equals(h.state)) freeze++;
            else watch++;
            s.append(h.summary()).append("\n");
        }
        s.insert(
                0,
                "KernelHealth plugins="
                        + REGISTRY.size()
                        + "|STRONG="
                        + strong
                        + "|READY="
                        + ready
                        + "|WATCH="
                        + watch
                        + "|FREEZE="
                        + freeze
                        + "\n");
        return s.toString();
    }

    private static List<Plugin> buildRegistry() {
        ArrayList<Plugin> x = new ArrayList<Plugin>();
        x.add(new Probe01());
        x.add(new Probe02());
        x.add(new Probe03());
        x.add(new Probe04());
        x.add(new Probe05());
        x.add(new Probe06());
        x.add(new Probe07());
        x.add(new Probe08());
        x.add(new Probe09());
        x.add(new Probe10());
        x.add(new Probe11());
        x.add(new Probe12());
        x.add(new Probe13());
        x.add(new Probe14());
        x.add(new Probe15());
        x.add(new Probe16());
        x.add(new Probe17());
        x.add(new Probe18());
        x.add(new Probe19());
        x.add(new Probe20());
        x.add(new Probe21());
        x.add(new Probe22());
        x.add(new Probe23());
        x.add(new Probe24());
        x.add(new Probe25());
        x.add(new Probe26());
        x.add(new Probe27());
        x.add(new Probe28());
        x.add(new Probe29());
        x.add(new Probe30());
        x.add(new Probe31());
        x.add(new Probe32());
        x.add(new Probe33());
        x.add(new Probe34());
        x.add(new Probe35());
        x.add(new Probe36());
        x.add(new Probe37());
        x.add(new Probe38());
        x.add(new Probe39());
        x.add(new Probe40());
        x.add(new Probe41());
        x.add(new Probe42());
        x.add(new Probe43());
        x.add(new Probe44());
        x.add(new Probe45());
        x.add(new Probe46());
        x.add(new Probe47());
        x.add(new Probe48());
        x.add(new Probe49());
        x.add(new Probe50());
        x.add(new Probe51());
        x.add(new Probe52());
        x.add(new Probe53());
        x.add(new Probe54());
        x.add(new Probe55());
        x.add(new Probe56());
        x.add(new Probe57());
        x.add(new Probe58());
        x.add(new Probe59());
        x.add(new Probe60());
        x.add(new Probe61());
        x.add(new Probe62());
        x.add(new Probe63());
        x.add(new Probe64());
        Collections.sort(
                x,
                new Comparator<Plugin>() {
                    public int compare(Plugin a, Plugin b) {
                        return a.id().compareTo(b.id());
                    }
                });
        return Collections.unmodifiableList(x);
    }

    private static final class Probe01 implements Plugin {
        public String id() {
            return "01_ShapeBalanceSupport";
        }

        public String group() {
            return "SHAPE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"shapeScore", "shapeConfidence", "shapeSupport"});
            double counter = numeric(c, new String[] {"shapeRisk", "shapeConflictRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe02 implements Plugin {
        public String id() {
            return "02_ShapeBalanceRobustness";
        }

        public String group() {
            return "SHAPE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"shapeSupport", "shapeConfidence", "shapeScore"});
            double counter = numeric(c, new String[] {"shapeConflictRisk", "shapeRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe03 implements Plugin {
        public String id() {
            return "03_ShapeMemorySupport";
        }

        public String group() {
            return "SHAPE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"memoryScore", "shapeMemoryScore", "similarScore"});
            double counter = numeric(c, new String[] {"memoryRisk", "similarRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe04 implements Plugin {
        public String id() {
            return "04_ShapeMemoryRobustness";
        }

        public String group() {
            return "SHAPE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"similarScore", "shapeMemoryScore", "memoryScore"});
            double counter = numeric(c, new String[] {"similarRisk", "memoryRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe05 implements Plugin {
        public String id() {
            return "05_SequenceTrendSupport";
        }

        public String group() {
            return "SEQUENCE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"trendScore", "trendConfidence", "sequenceScore"});
            double counter = numeric(c, new String[] {"driftRisk", "trendRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.7000d + (1.0d - counter) * 0.3000d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe06 implements Plugin {
        public String id() {
            return "06_SequenceTrendRobustness";
        }

        public String group() {
            return "SEQUENCE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"sequenceScore", "trendConfidence", "trendScore"});
            double counter = numeric(c, new String[] {"trendRisk", "driftRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe07 implements Plugin {
        public String id() {
            return "07_MarkovTransitionSupport";
        }

        public String group() {
            return "SEQUENCE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "markovScore", "transitionScore", "transitionConfidence"
                            });
            double counter = numeric(c, new String[] {"transitionRisk", "breakRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5950d + (1.0d - counter) * 0.4050d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe08 implements Plugin {
        public String id() {
            return "08_MarkovTransitionRobustness";
        }

        public String group() {
            return "SEQUENCE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "transitionConfidence", "transitionScore", "markovScore"
                            });
            double counter = numeric(c, new String[] {"breakRisk", "transitionRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe09 implements Plugin {
        public String id() {
            return "09_CausalFactorSupport";
        }

        public String group() {
            return "CAUSAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"causalScore", "causalConfidence", "causalSupport"});
            double counter = numeric(c, new String[] {"causalRisk", "counterRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6650d + (1.0d - counter) * 0.3350d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe10 implements Plugin {
        public String id() {
            return "10_CausalFactorRobustness";
        }

        public String group() {
            return "CAUSAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"causalSupport", "causalConfidence", "causalScore"});
            double counter = numeric(c, new String[] {"counterRisk", "causalRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe11 implements Plugin {
        public String id() {
            return "11_PositionCauseSupport";
        }

        public String group() {
            return "CAUSAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "positionCauseScore", "positionCausalScore", "positionSupport"
                            });
            double counter = numeric(c, new String[] {"positionCauseRisk", "positionRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe12 implements Plugin {
        public String id() {
            return "12_PositionCauseRobustness";
        }

        public String group() {
            return "CAUSAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "positionSupport", "positionCausalScore", "positionCauseScore"
                            });
            double counter = numeric(c, new String[] {"positionRisk", "positionCauseRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe13 implements Plugin {
        public String id() {
            return "13_TeacherMatrixSupport";
        }

        public String group() {
            return "TEACHER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "teacherMatrixScore", "teacherNumberScore", "teacherConfidence"
                            });
            double counter = numeric(c, new String[] {"teacherMatrixRisk", "teacherNumberRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe14 implements Plugin {
        public String id() {
            return "14_TeacherMatrixRobustness";
        }

        public String group() {
            return "TEACHER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "teacherConfidence", "teacherNumberScore", "teacherMatrixScore"
                            });
            double counter = numeric(c, new String[] {"teacherNumberRisk", "teacherMatrixRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe15 implements Plugin {
        public String id() {
            return "15_TeacherOrdinarySupport";
        }

        public String group() {
            return "TEACHER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "teacherScore", "teacherHitRate", "teacherRecentHitRate"
                            });
            double counter = numeric(c, new String[] {"teacherRisk", "teacherMatrixRiskPenalty"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.7000d + (1.0d - counter) * 0.3000d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe16 implements Plugin {
        public String id() {
            return "16_TeacherOrdinaryRobustness";
        }

        public String group() {
            return "TEACHER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "teacherRecentHitRate", "teacherHitRate", "teacherScore"
                            });
            double counter = numeric(c, new String[] {"teacherMatrixRiskPenalty", "teacherRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe17 implements Plugin {
        public String id() {
            return "17_CounterfactualRankSupport";
        }

        public String group() {
            return "COUNTERFACTUAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "counterfactualScore", "rankCounterfactualScore", "counterSupport"
                            });
            double counter = numeric(c, new String[] {"counterRisk", "counterEvidenceScore"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5950d + (1.0d - counter) * 0.4050d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe18 implements Plugin {
        public String id() {
            return "18_CounterfactualRankRobustness";
        }

        public String group() {
            return "COUNTERFACTUAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "counterSupport", "rankCounterfactualScore", "counterfactualScore"
                            });
            double counter = numeric(c, new String[] {"counterEvidenceScore", "counterRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe19 implements Plugin {
        public String id() {
            return "19_AblationFragilitySupport";
        }

        public String group() {
            return "COUNTERFACTUAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "v43StrategySupport", "v43StrategyDiversity", "v43StrategyMaturity"
                            });
            double counter = numeric(c, new String[] {"v43AblationFragility", "v43StrategyRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6650d + (1.0d - counter) * 0.3350d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe20 implements Plugin {
        public String id() {
            return "20_AblationFragilityRobustness";
        }

        public String group() {
            return "COUNTERFACTUAL";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "v43StrategyMaturity", "v43StrategyDiversity", "v43StrategySupport"
                            });
            double counter = numeric(c, new String[] {"v43StrategyRisk", "v43AblationFragility"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe21 implements Plugin {
        public String id() {
            return "21_DynamicClusterSupport";
        }

        public String group() {
            return "DYNAMIC";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "dynamicClusterScore", "dynamicClusterHitRate", "dynamicQuality"
                            });
            double counter = numeric(c, new String[] {"dynamicClusterRisk", "dynamicRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe22 implements Plugin {
        public String id() {
            return "22_DynamicClusterRobustness";
        }

        public String group() {
            return "DYNAMIC";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "dynamicQuality", "dynamicClusterHitRate", "dynamicClusterScore"
                            });
            double counter = numeric(c, new String[] {"dynamicRisk", "dynamicClusterRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe23 implements Plugin {
        public String id() {
            return "23_DynamicCauseSupport";
        }

        public String group() {
            return "DYNAMIC";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "dynamicCauseScore", "dynamicCauseConfidence", "v20Score"
                            });
            double counter = numeric(c, new String[] {"dynamicCauseRisk", "v20Risk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe24 implements Plugin {
        public String id() {
            return "24_DynamicCauseRobustness";
        }

        public String group() {
            return "DYNAMIC";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "v20Score", "dynamicCauseConfidence", "dynamicCauseScore"
                            });
            double counter = numeric(c, new String[] {"v20Risk", "dynamicCauseRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe25 implements Plugin {
        public String id() {
            return "25_CoverageQualitySupport";
        }

        public String group() {
            return "QUALITY";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "coverageQuality",
                                "coverageQualityScore",
                                "dynamicGateCoverageQualityScore"
                            });
            double counter = numeric(c, new String[] {"k3CoverageRisk", "coverageRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.7000d + (1.0d - counter) * 0.3000d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe26 implements Plugin {
        public String id() {
            return "26_CoverageQualityRobustness";
        }

        public String group() {
            return "QUALITY";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "dynamicGateCoverageQualityScore",
                                "coverageQualityScore",
                                "coverageQuality"
                            });
            double counter = numeric(c, new String[] {"coverageRisk", "k3CoverageRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe27 implements Plugin {
        public String id() {
            return "27_ThirdNumberSupport";
        }

        public String group() {
            return "QUALITY";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "thirdQuality",
                                "thirdNumberQualityScore",
                                "dynamicGateThirdNumberQuality"
                            });
            double counter = numeric(c, new String[] {"thirdNumberRisk", "thirdRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5950d + (1.0d - counter) * 0.4050d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe28 implements Plugin {
        public String id() {
            return "28_ThirdNumberRobustness";
        }

        public String group() {
            return "QUALITY";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "dynamicGateThirdNumberQuality",
                                "thirdNumberQualityScore",
                                "thirdQuality"
                            });
            double counter = numeric(c, new String[] {"thirdRisk", "thirdNumberRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe29 implements Plugin {
        public String id() {
            return "29_CalibrationSupport";
        }

        public String group() {
            return "CALIBRATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "calibrationConfidence",
                                "v46CalibrationConfidence",
                                "calibratedHitProbability"
                            });
            double counter = numeric(c, new String[] {"calibrationBrier", "calibrationEce"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6650d + (1.0d - counter) * 0.3350d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe30 implements Plugin {
        public String id() {
            return "30_CalibrationRobustness";
        }

        public String group() {
            return "CALIBRATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "calibratedHitProbability",
                                "v46CalibrationConfidence",
                                "calibrationConfidence"
                            });
            double counter = numeric(c, new String[] {"calibrationEce", "calibrationBrier"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe31 implements Plugin {
        public String id() {
            return "31_PosteriorSupport";
        }

        public String group() {
            return "CALIBRATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "posteriorProbability", "posteriorConfidence", "posteriorHitRate"
                            });
            double counter = numeric(c, new String[] {"posteriorRisk", "probabilityUncertainty"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe32 implements Plugin {
        public String id() {
            return "32_PosteriorRobustness";
        }

        public String group() {
            return "CALIBRATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "posteriorHitRate", "posteriorConfidence", "posteriorProbability"
                            });
            double counter = numeric(c, new String[] {"probabilityUncertainty", "posteriorRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe33 implements Plugin {
        public String id() {
            return "33_RegimeStabilitySupport";
        }

        public String group() {
            return "STATE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"stateScore", "stateConfidence", "trendLife"});
            double counter = numeric(c, new String[] {"driftRisk", "switchRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe34 implements Plugin {
        public String id() {
            return "34_RegimeStabilityRobustness";
        }

        public String group() {
            return "STATE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"trendLife", "stateConfidence", "stateScore"});
            double counter = numeric(c, new String[] {"switchRisk", "driftRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe35 implements Plugin {
        public String id() {
            return "35_LifecycleSupport";
        }

        public String group() {
            return "STATE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"lifecycleScore", "recoveryScore", "stabilityScore"});
            double counter = numeric(c, new String[] {"breakDepth", "lifecycleRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.7000d + (1.0d - counter) * 0.3000d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe36 implements Plugin {
        public String id() {
            return "36_LifecycleRobustness";
        }

        public String group() {
            return "STATE";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"stabilityScore", "recoveryScore", "lifecycleScore"});
            double counter = numeric(c, new String[] {"lifecycleRisk", "breakDepth"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe37 implements Plugin {
        public String id() {
            return "37_AssociationSupport";
        }

        public String group() {
            return "RELATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {"associationScore", "relationScore", "consensusScore"});
            double counter = numeric(c, new String[] {"associationConflictRisk", "relationRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5950d + (1.0d - counter) * 0.4050d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe38 implements Plugin {
        public String id() {
            return "38_AssociationRobustness";
        }

        public String group() {
            return "RELATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {"consensusScore", "relationScore", "associationScore"});
            double counter = numeric(c, new String[] {"relationRisk", "associationConflictRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe39 implements Plugin {
        public String id() {
            return "39_DistributionSupport";
        }

        public String group() {
            return "RELATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "distributionScore", "distributionConfidence", "densityScore"
                            });
            double counter = numeric(c, new String[] {"distributionRisk", "driftRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6650d + (1.0d - counter) * 0.3350d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe40 implements Plugin {
        public String id() {
            return "40_DistributionRobustness";
        }

        public String group() {
            return "RELATION";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "densityScore", "distributionConfidence", "distributionScore"
                            });
            double counter = numeric(c, new String[] {"driftRisk", "distributionRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe41 implements Plugin {
        public String id() {
            return "41_RuleHealthSupport";
        }

        public String group() {
            return "HEALTH";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {"ruleHealthScore", "moduleEffectScore", "healthScore"});
            double counter = numeric(c, new String[] {"ruleHealthRisk", "falseStrongRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe42 implements Plugin {
        public String id() {
            return "42_RuleHealthRobustness";
        }

        public String group() {
            return "HEALTH";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {"healthScore", "moduleEffectScore", "ruleHealthScore"});
            double counter = numeric(c, new String[] {"falseStrongRisk", "ruleHealthRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe43 implements Plugin {
        public String id() {
            return "43_ShadowPromotionSupport";
        }

        public String group() {
            return "HEALTH";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "shadowPromotionScore", "promotionConfidence", "recentEffectScore"
                            });
            double counter = numeric(c, new String[] {"shadowPromotionRisk", "freezeRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe44 implements Plugin {
        public String id() {
            return "44_ShadowPromotionRobustness";
        }

        public String group() {
            return "HEALTH";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "recentEffectScore", "promotionConfidence", "shadowPromotionScore"
                            });
            double counter = numeric(c, new String[] {"freezeRisk", "shadowPromotionRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe45 implements Plugin {
        public String id() {
            return "45_RankQualitySupport";
        }

        public String group() {
            return "RANK";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"rawRankScore", "rankScore", "championGap"});
            double counter = numeric(c, new String[] {"rankRisk", "rankUncertainty"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.7000d + (1.0d - counter) * 0.3000d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe46 implements Plugin {
        public String id() {
            return "46_RankQualityRobustness";
        }

        public String group() {
            return "RANK";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"championGap", "rankScore", "rawRankScore"});
            double counter = numeric(c, new String[] {"rankUncertainty", "rankRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe47 implements Plugin {
        public String id() {
            return "47_ModelEvidenceSupport";
        }

        public String group() {
            return "RANK";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"modelP", "rawP", "historicalPriorProbability"});
            double counter = numeric(c, new String[] {"modelRisk", "v46ProbabilityUncertainty"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5950d + (1.0d - counter) * 0.4050d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe48 implements Plugin {
        public String id() {
            return "48_ModelEvidenceRobustness";
        }

        public String group() {
            return "RANK";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"historicalPriorProbability", "rawP", "modelP"});
            double counter = numeric(c, new String[] {"v46ProbabilityUncertainty", "modelRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe49 implements Plugin {
        public String id() {
            return "49_Window12Support";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"window12Score", "r12", "recent12Score"});
            double counter = numeric(c, new String[] {"window12Risk", "recentRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6650d + (1.0d - counter) * 0.3350d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe50 implements Plugin {
        public String id() {
            return "50_Window12Robustness";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"recent12Score", "r12", "window12Score"});
            double counter = numeric(c, new String[] {"recentRisk", "window12Risk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe51 implements Plugin {
        public String id() {
            return "51_Window24Support";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"window24Score", "r24", "recent24Score"});
            double counter = numeric(c, new String[] {"window24Risk", "recentRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe52 implements Plugin {
        public String id() {
            return "52_Window24Robustness";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"recent24Score", "r24", "window24Score"});
            double counter = numeric(c, new String[] {"recentRisk", "window24Risk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe53 implements Plugin {
        public String id() {
            return "53_Window48Support";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"window48Score", "r48", "recent48Score"});
            double counter = numeric(c, new String[] {"window48Risk", "midRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe54 implements Plugin {
        public String id() {
            return "54_Window48Robustness";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"recent48Score", "r48", "window48Score"});
            double counter = numeric(c, new String[] {"midRisk", "window48Risk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe55 implements Plugin {
        public String id() {
            return "55_Window96Support";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"window96Score", "r96", "recent96Score"});
            double counter = numeric(c, new String[] {"window96Risk", "midRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.7000d + (1.0d - counter) * 0.3000d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe56 implements Plugin {
        public String id() {
            return "56_Window96Robustness";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"recent96Score", "r96", "window96Score"});
            double counter = numeric(c, new String[] {"midRisk", "window96Risk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe57 implements Plugin {
        public String id() {
            return "57_Window360Support";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"window360Score", "r360", "longScore"});
            double counter = numeric(c, new String[] {"window360Risk", "longRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5950d + (1.0d - counter) * 0.4050d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe58 implements Plugin {
        public String id() {
            return "58_Window360Robustness";
        }

        public String group() {
            return "WINDOW";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary = numeric(c, new String[] {"longScore", "r360", "window360Score"});
            double counter = numeric(c, new String[] {"longRisk", "window360Risk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe59 implements Plugin {
        public String id() {
            return "59_OmissionSupport";
        }

        public String group() {
            return "NUMBER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"omissionScore", "gapScore", "numberOmissionScore"});
            double counter = numeric(c, new String[] {"omissionRisk", "extremeGapRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6650d + (1.0d - counter) * 0.3350d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe60 implements Plugin {
        public String id() {
            return "60_OmissionRobustness";
        }

        public String group() {
            return "NUMBER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(c, new String[] {"numberOmissionScore", "gapScore", "omissionScore"});
            double counter = numeric(c, new String[] {"extremeGapRisk", "omissionRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe61 implements Plugin {
        public String id() {
            return "61_MomentumSupport";
        }

        public String group() {
            return "NUMBER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {"momentumScore", "numberMomentumScore", "hotColdScore"});
            double counter = numeric(c, new String[] {"momentumRisk", "overheatRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.5600d + (1.0d - counter) * 0.4400d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe62 implements Plugin {
        public String id() {
            return "62_MomentumRobustness";
        }

        public String group() {
            return "NUMBER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {"hotColdScore", "numberMomentumScore", "momentumScore"});
            double counter = numeric(c, new String[] {"overheatRisk", "momentumRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe63 implements Plugin {
        public String id() {
            return "63_NumberBehaviorSupport";
        }

        public String group() {
            return "NUMBER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "numberBehaviorScore", "numberConfidence", "behaviorSupport"
                            });
            double counter = numeric(c, new String[] {"numberBehaviorRisk", "numberConflictRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double support = clamp01(primary * 0.6300d + (1.0d - counter) * 0.3700d);
            double risk = clamp01(counter * 0.72d + Math.max(0.0d, 0.42d - primary) * 0.28d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static final class Probe64 implements Plugin {
        public String id() {
            return "64_NumberBehaviorRobustness";
        }

        public String group() {
            return "NUMBER";
        }

        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage) {
            long t = System.nanoTime();
            double primary =
                    numeric(
                            c,
                            new String[] {
                                "behaviorSupport", "numberConfidence", "numberBehaviorScore"
                            });
            double counter = numeric(c, new String[] {"numberConflictRisk", "numberBehaviorRisk"});
            int sample =
                    sample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "posteriorSample",
                                "teacherMatrixSample",
                                "teacherNumberSample",
                                "dynamicClusterSample",
                                "evidenceForestSample",
                                "microWalkForwardSample"
                            });
            double agreement = 1.0d - Math.min(1.0d, Math.abs(primary - 0.50d) * 1.45d);
            double support =
                    clamp01(primary * 0.46d + agreement * 0.31d + (1.0d - counter) * 0.23d);
            double risk =
                    clamp01(
                            counter * 0.58d
                                    + (1.0d - agreement) * 0.25d
                                    + Math.max(0.0d, 0.36d - primary) * 0.17d);
            double maturity = Math.min(1.0d, sample / 120.0d);
            double confidence =
                    clamp01(0.20d + 0.55d * maturity + 0.25d * Math.abs(support - risk));
            HealthSnapshot h = health(store, id());
            if ("FREEZE".equals(h.state)) risk = Math.max(risk, 0.72d);
            if ("STRONG".equals(h.state)) confidence = clamp01(confidence + 0.08d);
            return new EvidencePacket(
                    id(),
                    group(),
                    support,
                    risk,
                    confidence,
                    sample,
                    System.nanoTime() - t,
                    false,
                    h.state,
                    hash(id() + "|" + fmt(primary) + "|" + fmt(counter) + "|" + safe(stage)));
        }
    }

    private static EvidencePacket packet(
            DbStore store,
            String id,
            String group,
            double s,
            double r,
            double c,
            int n,
            long ns,
            boolean timeout,
            String status) {
        HealthSnapshot h = health(store, id);
        String st = safe(status).length() > 0 ? status : h.state;
        return new EvidencePacket(
                id,
                group,
                s,
                r,
                c,
                n,
                ns,
                timeout,
                st,
                hash(id + "|" + fmt(s) + "|" + fmt(r) + "|" + n));
    }

    private static HealthSnapshot health(DbStore store, String id) {
        int n = 0, h = 0;
        try {
            String k = healthKey(id);
            Map<String, String> snapshot = R923_HEALTH_SNAPSHOT.get();
            if (snapshot != null) {
                n = parseInt(snapshot.get(k + "_n"), 0);
                h = parseInt(snapshot.get(k + "_h"), 0);
            } else if (store != null) {
                n = parseInt(store.getState(k + "_n", "0"), 0);
                h = parseInt(store.getState(k + "_h", "0"), 0);
            }
        } catch (Throwable ignore) {
        }
        double r = n <= 0 ? 0.0d : (double) h / n;
        String st = "WATCH";
        if (n >= AppConfig.B07R3_KERNEL_READY_SAMPLE && r < AppConfig.B07R3_KERNEL_FREEZE_RATE)
            st = "FREEZE";
        else if (n >= AppConfig.B07R3_KERNEL_STRONG_SAMPLE
                && r >= AppConfig.B07R3_KERNEL_STRONG_RATE) st = "STRONG";
        else if (n >= AppConfig.B07R3_KERNEL_READY_SAMPLE && r >= AppConfig.B07R3_KERNEL_READY_RATE)
            st = "READY";
        return new HealthSnapshot(id, n, h, r, st);
    }

    private static String healthKey(String id) {
        return "starcore_kernel_plugin_" + machine(id);
    }

    private static double numeric(Object o, String[] names) {
        double sum = 0.0d;
        int n = 0;
        if (names != null)
            for (int i = 0; i < names.length; i++) {
                Double v = readNumber(o, names[i]);
                if (v == null) continue;
                double x = v.doubleValue();
                if (x > 1.5d && x <= 100.0d) x /= 100.0d;
                if (x >= 0.0d && x <= 1.0d) {
                    sum += x;
                    n++;
                }
            }
        return n == 0 ? 0.50d : clamp01(sum / n);
    }

    private static int sample(Object o, String[] names) {
        int m = 0;
        if (names != null)
            for (int i = 0; i < names.length; i++) {
                Double v = readNumber(o, names[i]);
                if (v != null && v.doubleValue() > m) m = (int) Math.round(v.doubleValue());
            }
        return Math.max(0, m);
    }

    private static Double readNumber(Object o, String name) {
        if (o == null || safe(name).length() == 0) return null;
        try {
            Field f = o.getClass().getField(name);
            Object v = f.get(o);
            if (v instanceof Number) return Double.valueOf(((Number) v).doubleValue());
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void writeDouble(Object o, String n, double v) {
        try {
            Field f = o.getClass().getField(n);
            if (f.getType() == Double.TYPE) f.setDouble(o, v);
            else f.set(o, Double.valueOf(v));
        } catch (Throwable ignore) {
        }
    }

    private static void writeInt(Object o, String n, int v) {
        try {
            Field f = o.getClass().getField(n);
            if (f.getType() == Integer.TYPE) f.setInt(o, v);
            else f.set(o, Integer.valueOf(v));
        } catch (Throwable ignore) {
        }
    }

    private static void writeString(Object o, String n, String v) {
        try {
            Field f = o.getClass().getField(n);
            f.set(o, safe(v));
        } catch (Throwable ignore) {
        }
    }

    private static void add(Map<String, Double> m, String k, double v) {
        Double old = m.get(k);
        m.put(k, Double.valueOf((old == null ? 0.0d : old.doubleValue()) + v));
    }

    private static double healthWeight(String h) {
        if ("STRONG".equals(h)) return 1.18d;
        if ("READY".equals(h)) return 1.0d;
        if ("FREEZE".equals(h)) return 0.20d;
        if ("ERROR".equals(h)) return 0.10d;
        return 0.72d;
    }

    private static void persistBundle(DbStore store, EvidenceBundle b, String stage) {
        if (store == null || b == null) return;
        try {
            int calls = parseInt(store.getState(STATE_CALLS, "0"), 0) + 1;
            int timeouts = parseInt(store.getState(STATE_TIMEOUTS, "0"), 0) + b.timedOutPlugins;
            store.setState(STATE_CALLS, String.valueOf(calls));
            store.setState(STATE_TIMEOUTS, String.valueOf(timeouts));
            store.setState(STATE_LAST, "stage=" + safe(stage) + "|" + b.summary());
            store.setState(STATE_HEALTH, b.healthState);
            store.registerModuleBuildV20("StarcoreEvidenceKernel", VERSION, b.summary());
        } catch (Throwable ignore) {
        }
    }

    private static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(safe(text).getBytes("UTF-8"));
            StringBuilder s = new StringBuilder(64);
            for (int i = 0; i < b.length; i++)
                s.append(String.format(Locale.US, "%02x", b[i] & 255));
            return s.toString();
        } catch (Throwable e) {
            return Integer.toHexString(safe(text).hashCode());
        }
    }

    private static String machine(String s) {
        return safe(s).replaceAll("[^A-Za-z0-9]+", "_").toLowerCase(Locale.US);
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String fmt(double v) {
        return String.valueOf(Math.round(clamp01(v) * 10000.0d) / 10000.0d);
    }

    private static String shortHash(String s) {
        return safe(s).length() <= 12 ? safe(s) : s.substring(0, 12);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final class R3KernelContractIndex {
        public static final String MARKER = "B07R3_KERNEL_CONTRACT_INDEX";

        public static final class Entry {
            public final String plugin, stage, input, output, forbidden;
            public final int budgetMicros;

            Entry(String p, String s, String i, String o, String f, int b) {
                plugin = p;
                stage = s;
                input = i;
                output = o;
                forbidden = f;
                budgetMicros = b;
            }

            public String text() {
                return plugin
                        + "@"
                        + stage
                        + "|"
                        + input
                        + "->"
                        + output
                        + "|deny="
                        + forbidden
                        + "|us="
                        + budgetMicros;
            }
        }

        public static java.util.List<Entry> all() {
            java.util.ArrayList<Entry> x = new java.util.ArrayList<Entry>();
            x.add(
                    new Entry(
                            "01_ShapeBalanceSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "02_ShapeBalanceRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "03_ShapeMemorySupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "04_ShapeMemoryRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            x.add(
                    new Entry(
                            "05_SequenceTrendSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1200));
            x.add(
                    new Entry(
                            "06_SequenceTrendRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1250));
            x.add(
                    new Entry(
                            "07_MarkovTransitionSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1300));
            x.add(
                    new Entry(
                            "08_MarkovTransitionRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1350));
            x.add(
                    new Entry(
                            "09_CausalFactorSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1400));
            x.add(
                    new Entry(
                            "10_CausalFactorRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1450));
            x.add(
                    new Entry(
                            "11_PositionCauseSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "12_PositionCauseRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "13_TeacherMatrixSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "14_TeacherMatrixRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            x.add(
                    new Entry(
                            "15_TeacherOrdinarySupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1200));
            x.add(
                    new Entry(
                            "16_TeacherOrdinaryRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1250));
            x.add(
                    new Entry(
                            "17_CounterfactualRankSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1300));
            x.add(
                    new Entry(
                            "18_CounterfactualRankRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1350));
            x.add(
                    new Entry(
                            "19_AblationFragilitySupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1400));
            x.add(
                    new Entry(
                            "20_AblationFragilityRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1450));
            x.add(
                    new Entry(
                            "21_DynamicClusterSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "22_DynamicClusterRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "23_DynamicCauseSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "24_DynamicCauseRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            x.add(
                    new Entry(
                            "25_CoverageQualitySupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1200));
            x.add(
                    new Entry(
                            "26_CoverageQualityRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1250));
            x.add(
                    new Entry(
                            "27_ThirdNumberSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1300));
            x.add(
                    new Entry(
                            "28_ThirdNumberRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1350));
            x.add(
                    new Entry(
                            "29_CalibrationSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1400));
            x.add(
                    new Entry(
                            "30_CalibrationRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1450));
            x.add(
                    new Entry(
                            "31_PosteriorSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "32_PosteriorRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "33_RegimeStabilitySupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "34_RegimeStabilityRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            x.add(
                    new Entry(
                            "35_LifecycleSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1200));
            x.add(
                    new Entry(
                            "36_LifecycleRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1250));
            x.add(
                    new Entry(
                            "37_AssociationSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1300));
            x.add(
                    new Entry(
                            "38_AssociationRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1350));
            x.add(
                    new Entry(
                            "39_DistributionSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1400));
            x.add(
                    new Entry(
                            "40_DistributionRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1450));
            x.add(
                    new Entry(
                            "41_RuleHealthSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "42_RuleHealthRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "43_ShadowPromotionSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "44_ShadowPromotionRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            x.add(
                    new Entry(
                            "45_RankQualitySupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1200));
            x.add(
                    new Entry(
                            "46_RankQualityRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1250));
            x.add(
                    new Entry(
                            "47_ModelEvidenceSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1300));
            x.add(
                    new Entry(
                            "48_ModelEvidenceRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1350));
            x.add(
                    new Entry(
                            "49_Window12Support",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1400));
            x.add(
                    new Entry(
                            "50_Window12Robustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1450));
            x.add(
                    new Entry(
                            "51_Window24Support",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "52_Window24Robustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "53_Window48Support",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "54_Window48Robustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            x.add(
                    new Entry(
                            "55_Window96Support",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1200));
            x.add(
                    new Entry(
                            "56_Window96Robustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1250));
            x.add(
                    new Entry(
                            "57_Window360Support",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1300));
            x.add(
                    new Entry(
                            "58_Window360Robustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1350));
            x.add(
                    new Entry(
                            "59_OmissionSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1400));
            x.add(
                    new Entry(
                            "60_OmissionRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1450));
            x.add(
                    new Entry(
                            "61_MomentumSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1000));
            x.add(
                    new Entry(
                            "62_MomentumRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1050));
            x.add(
                    new Entry(
                            "63_NumberBehaviorSupport",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1100));
            x.add(
                    new Entry(
                            "64_NumberBehaviorRobustness",
                            "PRE_PROBABILITY+PRE_DECISION+REPLAY",
                            "FeatureGraph+Candidate+PluginHealth",
                            "ImmutableEvidencePacket",
                            "FinalP,Lane,BET,Settlement",
                            1150));
            return x;
        }

        public static void validate(EvidenceBundle b) {
            if (b == null) return;
            java.util.List<Entry> x = all();
            if (x.size() < 60) {
                b.hardRisk = 1;
                b.healthState = "FREEZE";
                return;
            }
            if (b.availableGroups < AppConfig.B07R3_KERNEL_MIN_GROUPS) {
                b.hardRisk = 1;
                b.healthState = "FREEZE";
            }
            if (b.timedOutPlugins > Math.max(2, x.size() / 8)) {
                b.risk = clamp01(Math.max(b.risk, 0.72d));
                if (!"FREEZE".equals(b.healthState)) b.healthState = "WATCH";
            }
        }

        public static String manifest() {
            StringBuilder s = new StringBuilder(22000);
            java.util.List<Entry> x = all();
            for (int i = 0; i < x.size(); i++) s.append(x.get(i).text()).append('\n');
            return s.toString();
        }

        public static String fingerprint() {
            return StarcoreEvidenceKernel.hash(manifest());
        }
    }
}
