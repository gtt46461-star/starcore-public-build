package com.ethk3.novaonepro;

import java.util.HashMap;
import java.util.Map;

/** V42 candidate/reject waterfall diagnostics. Never creates BET. */
public final class OpportunityMinerEngine {
    public static final String MODULE_VERSION = AppConfig.V42_OPPORTUNITY_MINER_VERSION;

    public static final class Funnel {
        public int checked;
        public int eligible;
        public int normal;
        public int floor;
        public int probe;
        public double bestAcceptScore = -1.0d;
        public String bestNearSummary = "";
        public final HashMap<String, Integer> rejectCounts = new HashMap<String, Integer>();

        public void observe(OosEligibilityEngine.Evaluation e) {
            checked++;
            if (e == null) {
                add("EVALUATION_NULL");
                return;
            }
            if (e.eligible) {
                eligible++;
                if (AppConfig.BET_DOMAIN_NORMAL.equals(e.lane)) normal++;
                else if (AppConfig.BET_DOMAIN_QUALIFIED_FLOOR.equals(e.lane)) floor++;
                else if (AppConfig.BET_DOMAIN_REAL_BET_PROBE.equals(e.lane)) probe++;
            } else {
                add(e.code);
            }
            if (e.acceptScore > bestAcceptScore) {
                bestAcceptScore = e.acceptScore;
                bestNearSummary = e.summary();
            }
        }

        private void add(String code) {
            String k = safe(code).length() == 0 ? "UNKNOWN" : safe(code);
            Integer old = rejectCounts.get(k);
            rejectCounts.put(k, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
        }

        public String summary() {
            return "checked="
                    + checked
                    + "|eligible="
                    + eligible
                    + "|N/F/P="
                    + normal
                    + "/"
                    + floor
                    + "/"
                    + probe
                    + "|bestAccept="
                    + round(bestAcceptScore)
                    + "|topReject="
                    + topRejects(6)
                    + "|bestNear="
                    + bestNearSummary;
        }

        private String topRejects(int limit) {
            StringBuilder sb = new StringBuilder();
            HashMap<String, Integer> copy = new HashMap<String, Integer>(rejectCounts);
            for (int n = 0; n < limit && copy.size() > 0; n++) {
                String best = "";
                int count = -1;
                for (Map.Entry<String, Integer> e : copy.entrySet()) {
                    int v = e.getValue() == null ? 0 : e.getValue().intValue();
                    if (v > count || (v == count && e.getKey().compareTo(best) < 0)) {
                        best = e.getKey();
                        count = v;
                    }
                }
                if (best.length() == 0) break;
                if (sb.length() > 0) sb.append(',');
                sb.append(best).append('=').append(count);
                copy.remove(best);
            }
            return sb.toString();
        }
    }

    private OpportunityMinerEngine() {}

    public static void persist(DbStore store, Funnel funnel) {
        if (store == null || funnel == null) return;
        try {
            store.setState("v42_opportunity_miner_version", MODULE_VERSION);
            store.setState("v42_opportunity_funnel", funnel.summary());
            store.setState("v42_opportunity_funnel_summary", funnel.summary());
        } catch (Throwable ignore) {
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }
}
