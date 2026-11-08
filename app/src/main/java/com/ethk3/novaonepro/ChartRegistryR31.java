package com.ethk3.novaonepro;

import java.util.*;
import java.util.concurrent.*;

/**
 * R31-B09-C01/C02: the one registry of the 47 actual chart objects (29 legacy page panels + 18 research views).
 *
 * <p>Every consumer (the charts page, the report object index and machine tables) derives its id set from here; a
 * title is display text only and never routes or filters a panel. Each id declares its source owner, role, scope,
 * consumer kind, empty-state reason and byte/point budget. {@link PanelLoader} isolates panels: at most two reads at a
 * time, a per-panel deadline, and an epoch guard, so one blocked or malformed panel never stops the others and a
 * cancelled page never overwrites a newer one.</p>
 */
public final class ChartRegistryR31 {
    private ChartRegistryR31() {}
    public static final String VERSION = "R31_CHART_REGISTRY_V1";
    public enum Group { LEGACY_STATUS, LEGACY_CACHED, LEGACY_SNAPSHOT, LEGACY_R83, RESEARCH }

    public static final class Entry {
        public final String id, sourceOwner, role, scope, consumerKind, emptyState; public final Group group; public final int researchGroup; public final long byteBudget; public final int pointBudget;
        Entry(String id, Group group, String owner, String role, String scope, String consumerKind, String empty, int researchGroup, long bytes, int points) {
            this.id = id; this.group = group; sourceOwner = owner; this.role = role; this.scope = scope; this.consumerKind = consumerKind; emptyState = empty; this.researchGroup = researchGroup; byteBudget = bytes; pointBudget = points;
        }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", id); m.put("group", group.name()); m.put("sourceOwner", sourceOwner); m.put("role", role); m.put("scope", scope);
            m.put("consumerKind", consumerKind); m.put("emptyState", emptyState); m.put("researchGroup", (long) researchGroup); m.put("byteBudget", byteBudget); m.put("pointBudget", (long) pointBudget); return m;
        }
    }
    /** The 12 R83 panels actually mounted, in page order; the three retired R83 builders are never executed. */
    public static final List<String> R83_VISIBLE = Collections.unmodifiableList(Arrays.asList(
            "r83_algorithm_all", "r83_algorithm_rank", "r83_best_curve", "r83_action_candles", "r83_all_hit", "r83_act_hit",
            "r83_number_candles", "r83_number_line", "r83_omission_heat", "r83_hot_cold", "r83_calibration", "r83_brier20"));
    public static final List<String> R83_DRAW_INPUT = Collections.unmodifiableList(Arrays.asList("r83_number_candles", "r83_number_line", "r83_omission_heat", "r83_hot_cold"));
    private static final List<Entry> ALL;
    static {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry("status", Group.LEGACY_STATUS, "ChartsRepositoryR27.headline/headlineSnapshotR30", "REPORT_SNAPSHOT", "CURRENT_SNAPSHOT", "DIAGNOSTIC", "NO_SNAPSHOT_YET_WAIT_FOR_REPORT_TASK", 0, 64 << 10, 64));
        for (String id : com.ethk3.novaonepro.strategyv2.ChartCacheR27.PANELS)
            out.add(new Entry(id, Group.LEGACY_CACHED, "ChartCacheR27", "RESEARCH_DISPLAY", "R27_EVOLUTION_SELECT", "DIAGNOSTIC", "RESEARCH_THREAD_HAS_NOT_PRODUCED_THIS_PANEL", 0, 512 << 10, 2048));
        for (String id : com.ethk3.novaonepro.strategyv2.ReportChartsR30.IDS)
            out.add(new Entry(id, Group.LEGACY_SNAPSHOT, "ReportChartsR30", "RESEARCH_DISPLAY", "SAME_SOURCE_AS_REPORT", "DIAGNOSTIC", "NO_REPORT_SNAPSHOT_MISSING_DATA_IS_NOT_ZERO", 0, 256 << 10, 1024));
        for (String id : R83_VISIBLE) {
            String role = id.equals("r83_action_candles") ? "REAL_DECISION_AND_SETTLEMENT_ONLY" : id.equals("r83_calibration") || id.equals("r83_brier20") ? "PROBABILITY_DIAGNOSTIC" : id.startsWith("r83_algorithm") || id.equals("r83_best_curve") ? "SHADOW_AND_RESEARCH_BOARD" : "DRAW_HISTORY";
            out.add(new Entry(id, Group.LEGACY_R83, "ChartsRepositoryR83", role, R83_DRAW_INPUT.contains(id) ? "DRAWS_SHARED_INPUT" : "OWN_QUERY", "DIAGNOSTIC", "NO_ROWS_FOR_THIS_PANEL", 0, 256 << 10, 4096));
        }
        for (com.ethk3.novaonepro.strategyv2.ResearchGraphSpecsR30R1.Spec s : com.ethk3.novaonepro.strategyv2.ResearchGraphSpecsR30R1.ALL)
            out.add(new Entry(s.id, Group.RESEARCH, "ResearchObservatoryR30R1:" + s.measure, s.role, "RESEARCH_" + s.role, s.consumerKind.name(), "NO_PRODUCT_OR_NO_INPUT_FOR_THIS_VIEW", s.group, 256 << 10, 512));
        ALL = Collections.unmodifiableList(out);
        validateUniqueIds(ALL);
    }
    public static List<Entry> all() { return ALL; }
    public static Entry find(String id) { for (Entry e : ALL) if (e.id.equals(id)) return e; throw new IllegalArgumentException("R31_CHART_UNKNOWN_ID:" + id); }
    public static List<String> ids() { List<String> out = new ArrayList<>(); for (Entry e : ALL) out.add(e.id); return Collections.unmodifiableList(out); }
    public static List<String> visibleLegacyIds() { List<String> out = new ArrayList<>(); for (Entry e : ALL) if (e.group != Group.RESEARCH) out.add(e.id); return Collections.unmodifiableList(out); }
    public static List<String> researchGroup(int group) { List<String> out = new ArrayList<>(); for (Entry e : ALL) if (e.group == Group.RESEARCH && e.researchGroup == group) out.add(e.id); return Collections.unmodifiableList(out); }
    public static void validateUniqueIds(List<Entry> entries) {
        Set<String> seen = new HashSet<>(); for (Entry e : entries) if (!seen.add(e.id)) throw new IllegalStateException("R31_CHART_DUPLICATE_ID:" + e.id);
        if (entries.size() != 47) throw new IllegalStateException("R31_CHART_COUNT:" + entries.size());
    }

    /** Isolated panel loading with an epoch guard. */
    public static final class PanelLoader implements AutoCloseable {
        public enum Status { READY, FAILED, TIMEOUT, STALE_EPOCH }
        public static final class Outcome { public final String id; public final Status status; public final Object frame; public final String reason; public final long epoch;
            Outcome(String id, Status s, Object f, String r, long e) { this.id = id; status = s; frame = f; reason = r; epoch = e; } }
        public interface Source { Object load(String id) throws Exception; }
        private final ExecutorService pool; private volatile long epoch;
        public PanelLoader(int maxConcurrent) {
            if (maxConcurrent < 1 || maxConcurrent > 2) throw new IllegalArgumentException("R31_PANEL_LOADER_CONCURRENCY");
            pool = Executors.newFixedThreadPool(maxConcurrent, r -> { Thread t = new Thread(r, "r31-panel-loader"); t.setDaemon(true); return t; });
        }
        /** Starts a new page epoch; outcomes of earlier epochs are reported STALE_EPOCH and never delivered as current. */
        public long begin() { return ++epoch; }
        public long epoch() { return epoch; }
        public List<Outcome> load(long forEpoch, List<String> ids, Source source, long perPanelTimeoutMs) {
            List<Future<Object>> futures = new ArrayList<>();
            for (String id : ids) futures.add(pool.submit(() -> source.load(id)));
            List<Outcome> out = new ArrayList<>(); long deadline = System.nanoTime();
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i); Future<Object> f = futures.get(i);
                try {
                    Object frame = f.get(perPanelTimeoutMs, TimeUnit.MILLISECONDS);
                    out.add(forEpoch != epoch ? new Outcome(id, Status.STALE_EPOCH, null, "EPOCH_SUPERSEDED", forEpoch) : new Outcome(id, Status.READY, frame, "", forEpoch));
                } catch (TimeoutException slow) { f.cancel(true); out.add(new Outcome(id, Status.TIMEOUT, null, "PANEL_DEADLINE_EXCEEDED", forEpoch)); }
                catch (ExecutionException failed) { Throwable c = failed.getCause(); out.add(new Outcome(id, Status.FAILED, null, c.getClass().getSimpleName() + ":" + c.getMessage(), forEpoch)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); f.cancel(true); out.add(new Outcome(id, Status.FAILED, null, "INTERRUPTED", forEpoch)); }
                catch (CancellationException cancelled) { out.add(new Outcome(id, Status.FAILED, null, "CANCELLED", forEpoch)); }
            }
            return out;
        }
        @Override public void close() { pool.shutdownNow(); }
        public boolean awaitClosed(long ms) throws InterruptedException { return pool.awaitTermination(ms, TimeUnit.MILLISECONDS); }
    }
}
