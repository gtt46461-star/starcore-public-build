package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

public final class RecordedPolicyBirthR335Host {
    private static void need(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
 static void recordedBirthDoesNotBlockPeers(EvolutionLoopR27.Config cfg, FeatureCacheR27 cache,
                                             Map<?,?> original) throws Exception {
     String archived = null;
     for (Object item : (List<?>) original.get("pending")) {
         Map<?,?> candidate = (Map<?,?>) item;
         if ("BORN".equals(candidate.get("state"))) {
             archived = PolicySpecR27.parse((String) candidate.get("spec")).policyHash();
             break;
         }
     }
     need(archived != null, "fixture has an accepted candidate");
     try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
          EvolutionLoopR27 retried = new EvolutionLoopR27(cfg, cache, null, System::currentTimeMillis)) {
         SQLiteDatabase db = new SQLiteDatabase(connection);
         db.execSQL("CREATE TABLE r27_policy(policy_hash TEXT PRIMARY KEY,state TEXT,birth_generation INTEGER)");
         db.execSQL("INSERT INTO r27_policy VALUES(?, 'RETIRED', 2)", new Object[]{archived});
         retried.setSink(new StoreR27.EvolutionSink("HISTORY_RETRY", ""));
         retried.step();
         retried.step();
         retried.revalidateResearchAdmissions(db);
         retried.step();
         int rejected = 0, fresh = 0;
         Map<?,?> result = (Map<?,?>) JsonR27.parse(retried.snapshot());
         for (Object item : (List<?>) result.get("pending")) {
             Map<?,?> candidate = (Map<?,?>) item;
             if ("POLICY_HASH_ALREADY_RECORDED".equals(candidate.get("reason"))) {
                 need("DUPLICATE_BEHAVIOR".equals(candidate.get("state")), "prior policy must not be reborn");
                 need(archived.equals(PolicySpecR27.parse((String) candidate.get("spec")).policyHash()), "matching original hash");
                 rejected++;
             } else if ("BORN".equals(candidate.get("state"))) {
                 fresh++;
             }
         }
         need(retried.step == 4 && rejected > 0 && fresh > 0, "archived proposal blocked unrelated births");
     }
 }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(5000, 3185, new int[]{700,1900});
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config();
        cfg.seed = 3185;
        cfg.births = 128;
        cfg.nullControl = false;
        try (FeatureCacheR27 cache = new FeatureCacheR27(data);
             EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, cache, null, System::currentTimeMillis)) {
            loop.setSink(new StoreR27.EvolutionSink("REFERENCE", ""));
            for (int phase = 1; phase <= 3; phase++) loop.step();
            recordedBirthDoesNotBlockPeers(cfg, cache, (Map<?,?>) JsonR27.parse(loop.snapshot()));
        }
        System.out.println("RECORDED_POLICY_REJECTED_WITH_FRESH_PEERS_AND_CURSOR");
    }
}
