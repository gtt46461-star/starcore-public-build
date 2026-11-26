package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.BuildConfig;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class R343TypedRuleReportAndroidTest {
    private static final String DISCOVERY = StatsR27.sha256("typed discovery fixture");
    private static final String TARGET = StatsR27.sha256("later development fixture");
    private static final String OTHER = StatsR27.sha256("second later fixture");
    private static final long USE_AT = 100;

    @Test public void unavailableSchemaAndMissingProducerRemainUnknown() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            Map<String,Object> stages = stages(db);
            assertUnknown(stages, "retainedRulesAllSources", "ORIGINAL_SCHEMA_UNAVAILABLE");
            assertUnknown(stages, "exactConsumedChildren", "ORIGINAL_SCHEMA_UNAVAILABLE");
            assertMissingProducers(stages);
        }
    }

    @Test public void distinctTransferOriginalsAndExactDevelopmentEvidence() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            for (String ddl : ResearchSchemaR30R1.DDL) db.execSQL(ddl);
            for (String ddl : AtomPublicationStoreR31.ruleDdlR342()) db.execSQL(ddl);
            for (String ddl : SchemaR27.DDL) {
                if (ddl.startsWith("CREATE TABLE IF NOT EXISTS r27_replay_score(")) db.execSQL(ddl);
            }
            Map<String,Object> empty = stages(db);
            assertObserved(empty, "retainedRulesAllSources", 0);
            assertObserved(empty, "persistedTransferProposals", 0);
            assertObserved(empty, "exactConsumedChildren", 0);
            assertMissingProducers(empty);

            TypedGenerationRuleR342 rule = rule(FrozenConfirmationR30.currentSemantics());
            insertRule(db, rule);
            insertRule(db, rule("retained-historical-abi"));
            String first = rule.instantiate(TARGET, 2000, 2300, 2).get(0).policyHash();
            String second = rule.instantiate(TARGET, 2000, 2300, 2).get(1).policyHash();
            String production = BuildConfig.SOURCE_COMMIT_R26;
            proposal(db, rule, "exact", TARGET, 2000, first, production, "CONSUMED", false);
            consumption(db, rule, "exact", TARGET, first, false);
            proposal(db, rule, "ready", TARGET, 2000, second, production, "READY", false);
            proposal(db, rule, "mismatch", OTHER, 2000, second, production, "CONSUMED", false);
            consumption(db, rule, "mismatch", OTHER, second, true);
            proposal(db, rule, "prior", OTHER, 2000, first, StatsR27.sha256("prior source").substring(0, 40), "CONSUMED", false);
            consumption(db, rule, "prior", OTHER, first, false);
            proposal(db, rule, "same-dataset", DISCOVERY, 2000, first, production, "READY", false);
            proposal(db, rule, "overlap", OTHER, rule.through, first, production, "READY", false);
            proposal(db, rule, "malformed", OTHER, 2000, first, production, "READY", true);

            score(db, first, TARGET, SplitR27.TRAIN, USE_AT + 1);
            score(db, first, TARGET, SplitR27.HOLDOUT, USE_AT + 1);
            score(db, first, OTHER, SplitR27.SELECT, USE_AT + 1);
            score(db, second, OTHER, SplitR27.SELECT, USE_AT + 1);
            score(db, first, TARGET, SplitR27.SELECT, USE_AT - 1);
            Map<String,Object> observed = stages(db);
            assertObserved(observed, "retainedRulesAllSources", 2);
            assertObserved(observed, "retainedRulesCurrentAbi", 1);
            assertObserved(observed, "persistedTransferProposals", 3);
            assertObserved(observed, "distinctTransferDatasets", 2);
            assertObserved(observed, "exactConsumedChildren", 1);
            assertObserved(observed, "childrenWithRetainedDevScores", 0);
            assertMissingProducers(observed);

            score(db, first, TARGET, SplitR27.SELECT, USE_AT + 1);
            assertObserved(stages(db), "childrenWithRetainedDevScores", 1);
        }
    }

    private static TypedGenerationRuleR342 rule(String abi) {
        return TypedGenerationRuleR342.induct(policy("(add (zscore (freq 1 8)) (zscore (trans1)))"),
                StatsR27.sha256("first donor receipt"), policy("(add (zscore (freqnb 1 32)) (zscore (trans2)))"),
                StatsR27.sha256("second donor receipt"), DISCOVERY, 2, 1000, 1999, abi);
    }

    private static PolicySpecR27 policy(String ast) {
        return new PolicySpecR27(PolicyAstR27.parse(ast), 1, PolicySpecR27.Rule.QUANTILE,
                0d, .15d, 1d, true, .01d, .01d, IdeaFamilyR27.meta("I09", "REPORT_SQL_FIXTURE", 1, 18L, "isolated"));
    }

    private static void insertRule(SQLiteDatabase db, TypedGenerationRuleR342 rule) {
        db.execSQL("INSERT INTO " + AtomPublicationStoreR31.RULES_R342 + " VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[]{rule.ruleId, rule.shapeHash, rule.dataset, rule.revision, rule.abi, rule.from, rule.through,
                        rule.encodeCanonical(), 10L});
    }

    private static void proposal(SQLiteDatabase db, TypedGenerationRuleR342 rule, String name, String dataset,
            long from, String child, String source, String state, boolean malformed) {
        String run = StatsR27.sha256("run:" + name);
        String proposal = StatsR27.sha256("proposal:" + name);
        String parent = rule.parents().get(0);
        String summary = JsonR27.write(Map.of("sourceContract", Map.of("abi", rule.abi)));
        String payload = malformed ? "{" : JsonR27.write(Map.of("candidateHash", child,
                "typedRule", rule.ruleId, "ruleTransferDataset", dataset));
        db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", new Object[]{run, dataset,
                parent, "SELECT", source, 4, from, from + 300, StatsR27.sha256(summary), "COMMITTED", summary, 20L});
        db.execSQL("INSERT INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)", new Object[]{proposal, run, parent,
                "TYPED_GENERATION_RULE", payload, StatsR27.sha256(payload), state, "isolated test fixture", 30L});
    }

    private static void consumption(SQLiteDatabase db, TypedGenerationRuleR342 rule, String name,
            String dataset, String child, boolean mismatchedOriginal) {
        String run = StatsR27.sha256("run:" + name);
        String proposal = StatsR27.sha256("proposal:" + name);
        String consumer = "ORIGINAL_BIRTH_GATE_R30_REPAIR1";
        String useReceipt = HashingSU1.fields("R342_RULE_BORN_USE", rule.ruleId, dataset, run, proposal, child, "2");
        String consumeReceipt = ResearchContractR30R1.hash(proposal + '|' + child + "|2|" + consumer);
        db.execSQL("INSERT INTO " + AtomPublicationStoreR31.RULE_USES_R342 + " VALUES(?,?,?,?,?,?,?,?)",
                new Object[]{proposal, rule.ruleId, child, dataset, 2, run, useReceipt, USE_AT});
        db.execSQL("INSERT INTO r30r1_consume VALUES(?,?,?,?,?,?)",
                new Object[]{proposal, child, 2, consumer, consumeReceipt, USE_AT});
        String link = JsonR27.write(Map.of("typedRuleUseReceipt", mismatchedOriginal ? "different proof" : useReceipt,
                "receiptHash", consumeReceipt));
        db.execSQL("INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{run, "CONSUMED_BY:" + proposal, "RESEARCH_SOURCE_CONSUMPTION", child,
                        "ORIGINAL_BIRTH_GATE_RECEIPT", 2000, 2300, 0, null, null, null, link});
    }

    private static void score(SQLiteDatabase db, String child, String dataset, String split, long time) {
        db.execSQL("INSERT OR REPLACE INTO r27_replay_score(policy_hash,dataset_hash,split,n_sched,n_act,hits,"
                + "max_losing_streak,paired_u_mean,paired_u_lcb,computed_at) VALUES(?,?,?,40,20,12,1,0.2,0.1,?)",
                new Object[]{child, dataset, split, time});
    }

    private static Map<String,Object> stages(SQLiteDatabase db) {
        Map<String,Object> report = ResearchConsumptionStoreR31.report(db);
        return ResearchConsumptionStoreR31.object(ResearchConsumptionStoreR31.object(report.get("typedGenerationR343")).get("stages"));
    }

    private static void assertObserved(Map<String,Object> stages, String key, long count) {
        Map<String,Object> stage = ResearchConsumptionStoreR31.object(stages.get(key));
        assertEquals(key, "OBSERVED_RETAINED_ROWS", stage.get("status"));
        assertEquals(key, count, stage.get("count"));
    }

    private static void assertUnknown(Map<String,Object> stages, String key, String reason) {
        Map<String,Object> stage = ResearchConsumptionStoreR31.object(stages.get(key));
        assertEquals(key, "UNKNOWN", stage.get("status"));
        assertEquals(key, reason, stage.get("reason"));
        assertNull(key, stage.get("count"));
    }

    private static void assertMissingProducers(Map<String,Object> stages) {
        assertUnknown(stages, "eligibleDonorPairs", "DONOR_PAIR_ATTEMPT_NOT_PERSISTED");
        assertUnknown(stages, "incompatibleTypedSlotPairs", "INDUCTION_REJECTION_NOT_PERSISTED");
        assertUnknown(stages, "rawChildProposals", "PRE_STAGE_GENERATOR_OUTPUT_NOT_PERSISTED");
        assertUnknown(stages, "independentFutureResults", "NO_RULE_USE_TO_INDEPENDENT_FUTURE_BINDING");
    }
}
