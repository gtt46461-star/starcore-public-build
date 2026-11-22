package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class R335AppliedBudgetTableTest {
    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    static int checkExisting(SQLiteDatabase db) {
        int applied = 0;
        long offset = 0;
        long total;
        boolean more;
        do {
            Map<String,Object> page = ResearchTableAdaptersR30R1.readPageR31(db, "T09",
                    Collections.singletonMap("role", "SELECT"), offset, 2, null);
            require("READ_OK".equals(page.get("status")), "T09 " + page.get("status")
                    + " at " + offset + " " + page.get("reason"));
            total = ((Number)page.get("totalMatchingRows")).longValue();
            for (Object item : (List<?>)page.get("rows")) {
                Map<?,?> row = (Map<?,?>)item;
                if (!String.valueOf(row.get("measure_key")).startsWith("BUDGET_APPLIED_GENERATION:")) continue;
                Map<?,?> body = (Map<?,?>)JsonR27.parse((String)row.get("payload_json"));
                require(row.get("run_id").equals(body.get("sourceRun")), "source run");
                List<?> projected = (List<?>)row.get("objects");
                require(projected.size() == MutationOpsR27.OPS.length, "operator count");
                for (int i = 0; i < projected.size(); i++) {
                    Map<?,?> obj = (Map<?,?>)projected.get(i);
                    require(MutationOpsR27.OPS[i].equals(obj.get("operator")), "operator order");
                    require(((List<?>)body.get("beforeWeights")).get(i).equals(obj.get("beforeWeight")), "before");
                    require(((List<?>)body.get("afterWeights")).get(i).equals(obj.get("afterWeight")), "after");
                    require(!obj.containsKey("mean_gain"), "budget displayed as predictive gain");
                }
                applied++;
            }
            more = Boolean.TRUE.equals(page.get("hasMore"));
            long next = ((Number)page.get("nextOffset")).longValue();
            require(next > offset && next <= total, "read cursor");
            offset = next;
        } while (more);
        require(offset == total && applied > 0, "missing original budget record");
        return applied;
    }

    @Test public void originalOldAndCurrentBudgetRowsReopenBesideTheR16Frame() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE r30r1_analysis_run(run_id TEXT,role TEXT,policy_hash TEXT,recorded_at INTEGER)");
            db.execSQL("CREATE TABLE r30r1_research_measure(run_id TEXT,measure_key TEXT,kind TEXT,mean_gain REAL,payload_json TEXT)");
            String run = StatsR27.sha256("T09-budget");
            db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,'SELECT','parent',0)", new Object[]{run});
            db.execSQL("INSERT INTO r30r1_research_measure VALUES(?,'R16','RESEARCH_BUDGET',NULL,?)",
                    new Object[]{run, "{\"viewId\":\"R16\",\"frame\":{\"points\":[]}}"});
            List<Double> before = new ArrayList<>(), after = new ArrayList<>();
            for (int i = 0; i < MutationOpsR27.OPS.length; i++) {
                before.add(i + 0.5); after.add(1.0 / MutationOpsR27.OPS.length);
            }
            Map<String,Object> receipt = new LinkedHashMap<>();
            receipt.put("sourceRun", run); receipt.put("role", "SELECT");
            receipt.put("beforeWeights", before); receipt.put("afterWeights", after);
            receipt.put("version", "RESEARCH_PAIRED_BUDGET1");
            db.execSQL("INSERT INTO r30r1_research_measure VALUES(?,'BUDGET_APPLIED_GENERATION:1','RESEARCH_BUDGET',NULL,?)",
                    new Object[]{run, JsonR27.write(receipt)});
            receipt.put("version", "RESEARCH_PAIRED_BUDGET2_R335"); receipt.put("fixedPriors", before);
            db.execSQL("INSERT INTO r30r1_research_measure VALUES(?,'BUDGET_APPLIED_GENERATION:2','RESEARCH_BUDGET',NULL,?)",
                    new Object[]{run, JsonR27.write(receipt)});
            require(checkExisting(db) == 2, "generation rows");
            receipt.put("sourceRun", StatsR27.sha256("unrelated-run"));
            db.execSQL("UPDATE r30r1_research_measure SET payload_json=? WHERE measure_key='BUDGET_APPLIED_GENERATION:1'",
                    new Object[]{JsonR27.write(receipt)});
            Map<String,Object> invalid = ResearchTableAdaptersR30R1.readPageR31(db, "T09",
                    Collections.emptyMap(), 0, 1, null);
            require("READ_ERROR".equals(invalid.get("status")), "mismatched source passed");
            require(Boolean.TRUE.equals(invalid.get("hasMore"))
                    && ((Number)invalid.get("nextOffset")).intValue() == 1, "other intact rows became inaccessible");
            receipt.put("sourceRun", run); receipt.put("afterWeights", Collections.singletonList(0.0));
            db.execSQL("UPDATE r30r1_research_measure SET payload_json=? WHERE measure_key='BUDGET_APPLIED_GENERATION:1'",
                    new Object[]{JsonR27.write(receipt)});
            invalid = ResearchTableAdaptersR30R1.readPageR31(db, "T09", Collections.emptyMap(), 0, 1, null);
            require("READ_ERROR".equals(invalid.get("status")), "incomplete operator vector passed");
            Map<String,Object> next = ResearchTableAdaptersR30R1.readPageR31(db, "T09", Collections.emptyMap(), 1, 2, null);
            require("READ_OK".equals(next.get("status")) && ((Number)next.get("shown")).intValue()==2, "later valid rows");
        }
    }
}
