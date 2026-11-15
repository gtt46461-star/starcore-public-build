package com.ethk3.novaonepro;

import android.database.Cursor;

/** Small before/after snapshot proving whether one autonomous research pulse changed durable state. */
public final class ResearchEvolutionDeltaR922 {
    public static final String VERSION = "R922_RESEARCH_EVOLUTION_DELTA_V1";

    private ResearchEvolutionDeltaR922() {}

    public static Snapshot capture(DbStore store) {
        if (store == null) return Snapshot.empty();
        return new Snapshot(
                scalar(store, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R906_RESEARCH_ASSET),
                scalar(store, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R906_RESEARCH_ASSET
                        + " WHERE tier='CERTIFIED_SHADOW'"),
                scalar(store, "SELECT COUNT(1) FROM " + AssetGenesisEngineR915.TABLE_ASSET_GENOME),
                scalar(store, "SELECT COALESCE(SUM(future_n),0) FROM "
                        + AssetGenesisEngineR915.TABLE_ASSET_GENOME),
                scalar(store, "SELECT COUNT(1) FROM " + AssetGenesisEngineR915.TABLE_ASSET_GENOME
                        + " WHERE status='CERTIFIED_FUTURE_OOS'"),
                scalar(store, "SELECT COALESCE(SUM(state_updates),0) FROM "
                        + AssetGenesisEngineR915.TABLE_ASSET_GENOME),
                scalar(store, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME),
                scalar(store, "SELECT COALESCE(SUM(future_n),0) FROM "
                        + DbHelper.TABLE_R908_ALGORITHM_GENOME),
                scalar(store, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                        + " WHERE status='CHALLENGER_FUTURE_OOS'"),
                scalar(store, "SELECT COALESCE(SUM(state_updates),0) FROM "
                        + DbHelper.TABLE_R908_ALGORITHM_GENOME),
                parse(store.getState("r811_ftrl_updates",
                        store.getState("r817_research_adaptive_updates", "0"))),
                parse(store.getState("r909_tiny_deep_updates", "0")));
    }

    public static String publish(DbStore store, Snapshot before, Snapshot after, String reason) {
        Snapshot a = before == null ? Snapshot.empty() : before;
        Snapshot b = after == null ? Snapshot.empty() : after;
        String delta = VERSION + "|reason=" + safe(reason)
                + "|formulas=" + a.formulas + "->" + b.formulas
                + "|formulaCertified=" + a.formulaCertified + "->" + b.formulaCertified
                + "|assets=" + a.assets + "->" + b.assets
                + "|assetFuture=" + a.assetFuture + "->" + b.assetFuture
                + "|assetCertified=" + a.assetCertified + "->" + b.assetCertified
                + "|assetStateUpdates=" + a.assetStateUpdates + "->" + b.assetStateUpdates
                + "|algorithms=" + a.algorithms + "->" + b.algorithms
                + "|algorithmFuture=" + a.algorithmFuture + "->" + b.algorithmFuture
                + "|algorithmCertified=" + a.algorithmCertified + "->" + b.algorithmCertified
                + "|algorithmStateUpdates=" + a.algorithmStateUpdates + "->" + b.algorithmStateUpdates
                + "|FTRL=" + a.ftrlUpdates + "->" + b.ftrlUpdates
                + "|TinyDeep=" + a.tinyDeepUpdates + "->" + b.tinyDeepUpdates
                + "|changed=" + (a.equalsSnapshot(b) ? 0 : 1);
        try {
            if (store != null) {
                store.setState("r922_research_last_delta", delta);
                store.setState("r922_research_last_delta_at", String.valueOf(System.currentTimeMillis()));
                if (!a.equalsSnapshot(b)) {
                    store.setState("r922_research_last_material_change", delta);
                    store.setState("r922_research_last_material_change_at",
                            String.valueOf(System.currentTimeMillis()));
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (store != null && !RealtimePressureGateR811.hasActiveCriticalWork()) {
                ResearchEvolutionHeartbeatR947.Result heartbeat =
                        ResearchEvolutionHeartbeatR947.pulse(store, 700L);
                store.setState("r947_evolution_heartbeat", heartbeat.canonical());
            }
        } catch (ResearchWorkBudgetR927.Yield yielded) {
            try { if (store != null) store.setState("r947_evolution_heartbeat", "YIELD:" + yielded.getMessage()); } catch (Throwable ignored) {}
        } catch (Throwable failure) {
            try { if (store != null) store.setState("r947_evolution_heartbeat", "ERROR:" + failure.getClass().getSimpleName()); } catch (Throwable ignored) {}
        }
        return delta;
    }

    private static int scalar(DbStore store, String sql) {
        Cursor c = null;
        try {
            c = store.readable().rawQuery(sql, null);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (Throwable ignored) {
            return 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static int parse(String value) {
        try { return Math.max(0, Integer.parseInt(safe(value))); }
        catch (Throwable ignored) { return 0; }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Snapshot {
        public final int formulas, formulaCertified;
        public final int assets, assetFuture, assetCertified;
        public final int assetStateUpdates;
        public final int algorithms, algorithmFuture, algorithmCertified;
        public final int algorithmStateUpdates;
        public final int ftrlUpdates, tinyDeepUpdates;

        Snapshot(int formulas, int formulaCertified, int assets, int assetFuture,
                int assetCertified, int assetStateUpdates, int algorithms, int algorithmFuture,
                int algorithmCertified, int algorithmStateUpdates, int ftrlUpdates,
                int tinyDeepUpdates) {
            this.formulas = Math.max(0, formulas);
            this.formulaCertified = Math.max(0, formulaCertified);
            this.assets = Math.max(0, assets);
            this.assetFuture = Math.max(0, assetFuture);
            this.assetCertified = Math.max(0, assetCertified);
            this.assetStateUpdates = Math.max(0, assetStateUpdates);
            this.algorithms = Math.max(0, algorithms);
            this.algorithmFuture = Math.max(0, algorithmFuture);
            this.algorithmCertified = Math.max(0, algorithmCertified);
            this.algorithmStateUpdates = Math.max(0, algorithmStateUpdates);
            this.ftrlUpdates = Math.max(0, ftrlUpdates);
            this.tinyDeepUpdates = Math.max(0, tinyDeepUpdates);
        }

        static Snapshot empty() {
            return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        boolean equalsSnapshot(Snapshot other) {
            return other != null && formulas == other.formulas
                    && formulaCertified == other.formulaCertified
                    && assets == other.assets && assetFuture == other.assetFuture
                    && assetCertified == other.assetCertified && algorithms == other.algorithms
                    && assetStateUpdates == other.assetStateUpdates
                    && algorithmFuture == other.algorithmFuture
                    && algorithmCertified == other.algorithmCertified
                    && algorithmStateUpdates == other.algorithmStateUpdates
                    && ftrlUpdates == other.ftrlUpdates && tinyDeepUpdates == other.tinyDeepUpdates;
        }
    }
}
