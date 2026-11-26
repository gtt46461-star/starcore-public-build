package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Runs the R343-028 donor selection on the Android writer with synthetic development scores. */
@RunWith(AndroidJUnit4.class)
public final class R343TypedRuleDonorAndroidTest {
    private static final String LOOP = "R343_TYPED_RULE_DONOR_REGRESSION";

    @Test public void selectsOnlyEligiblePersistedDevelopmentDonors() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        String sandbox = InstrumentationRegistry.getArguments().getString("r30Sandbox");
        assertNotNull(sandbox);
        assertTrue(app.getApplicationInfo().dataDir.contains("/r30-instrumentation/" + sandbox + "/"));
        DbStore store = new DbStore(app);
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(LOOP, db -> {
                RuntimeSchemaR26.ensure(db);
                SchemaR27.ensure(db);
                for (String ddl : AtomPublicationStoreR31.ruleDdlR342()) db.execSQL(ddl);
                return 0;
            });
            ReplayDatasetR27 data = developmentHistory();
            SplitR27 split = SplitR27.of(data, null);
            long revision = SchemaR27.liveSourceRevision(store.readable());
            ResearchWriteQueueR27.Batch pin = new ResearchWriteQueueR27.Batch();
            ResearchInputPinR30R1.append(pin, data, revision, 1L);
            write(store, pin);
            assertEquals(revision, ResearchInputPinR30R1.admittedRevisionR341(store.readable(), data.datasetHash));

            PolicySpecR27 first = policy("(add (zscore (freq 1 8)) (zscore (trans1)))");
            PolicySpecR27 second = policy("(add (zscore (freqnb 1 32)) (zscore (trans2)))");
            try (EvalContextR27 eval = new EvalContextR27(new FeatureCacheR27(data), split, null)) {
                StoreR27.EvolutionSink wrongRoles = new StoreR27.EvolutionSink(LOOP, data.issues[0]);
                wrongRoles.policy(first, "ACTIVE", LOOP, 1, data.datasetHash);
                wrongRoles.policy(second, "ACTIVE", LOOP, 1, data.datasetHash);
                wrongRoles.score(first.policyHash(), data.datasetHash, score(SplitR27.TRAIN, 40, 40, 0.2));
                wrongRoles.score(second.policyHash(), data.datasetHash, score(SplitR27.HOLDOUT, 40, 40, 0.2));
                wrongRoles.score(second.policyHash(), StatsR27.sha256("other fixture"), score(SplitR27.SELECT, 40, 40, 0.2));
                write(store, wrongRoles.batch);
                assertNull("Train, holdout and a different dataset must stay out of discovery",
                        AtomPublicationStoreR31.prepareRuleR342(store.readable(), eval));

                putScores(store, eval, first, second, 29, 29, 0.2);
                assertNull("Too few scheduled development periods", AtomPublicationStoreR31.prepareRuleR342(store.readable(), eval));
                putScores(store, eval, first, second, 30, 0, 0.2);
                assertNull("No development actions", AtomPublicationStoreR31.prepareRuleR342(store.readable(), eval));
                putScores(store, eval, first, second, 30, 30, 0.0);
                assertNull("Nonpositive lower bound", AtomPublicationStoreR31.prepareRuleR342(store.readable(), eval));
                putScores(store, eval, first, second, 30, 30, 0.2);
                assertEquals(2, eligibleScoreRows(store.readable(), data.datasetHash));
                TypedGenerationRuleR342 rule = AtomPublicationStoreR31.prepareRuleR342(store.readable(), eval);
                assertNotNull("Two eligible H_SELECT scores must be visible to the production rule owner", rule);
                assertEquals(new HashSet<>(Arrays.asList(first.policyHash(), second.policyHash())), new HashSet<>(rule.parents()));
                assertEquals(FrozenConfirmationR30.currentSemantics(), rule.abi);
                assertEquals(data.datasetHash, rule.dataset);
                assertEquals(data.ordinals[split.trainFrom], rule.from);
                assertEquals(data.ordinals[split.selectTo - 1], rule.through);
                assertEquals(revision, rule.revision);
                assertTrue(!rule.firstProof.equals(rule.secondProof));
                List<PolicySpecR27> children = rule.instantiate(StatsR27.sha256("later fixture"),
                        rule.through + 1, rule.through + 200, 2);
                assertEquals(2, children.size());
                assertTrue(!FormulaIdentityR341.structure(children.get(0)).equals(FormulaIdentityR341.structure(children.get(1))));
            }
        } finally {
            store.close();
        }
    }

    private static ReplayDatasetR27 developmentHistory() {
        ReplayDatasetR27 synthetic = ReplayDatasetR27.synthetic(200, 343028L, new int[0]);
        long start = TargetIssueResolver.sequenceOrdinalSU1("202611240001");
        List<ReplayDatasetR27.Row> rows = new ArrayList<>(synthetic.size());
        for (int i = 0; i < synthetic.size(); i++) {
            long ordinal = start + i;
            rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(ordinal), ordinal, synthetic.draws[i]));
        }
        return ReplayDatasetR27.of(rows);
    }

    private static PolicySpecR27 policy(String ast) {
        return new PolicySpecR27(PolicyAstR27.parse(ast), 1, PolicySpecR27.Rule.QUANTILE,
                0d, .15d, 1d, true, .01d, .01d,
                IdeaFamilyR27.meta("I09", LOOP, 1, 343028L, "Synthetic database exercise"));
    }

    private static ReplayScoreR27 score(String split, int scheduled, int actions, double utility) {
        ReplayEvaluatorR27.Trace trace = new ReplayEvaluatorR27.Trace(scheduled, 0, scheduled, 1, 1d);
        for (int i = 0; i < scheduled; i++) {
            trace.skipped[i] = false;
            trace.act[i] = i < actions;
            trace.hit[i] = (byte) (trace.act[i] && i % 2 == 0 ? 1 : 0);
            trace.pHit[i] = .3d;
            trace.pActual[i] = .1d;
            trace.u[i] = utility;
        }
        return ReplayScoreR27.of(split, trace, 0, scheduled);
    }

    private static void putScores(DbStore store, EvalContextR27 eval, PolicySpecR27 first,
            PolicySpecR27 second, int scheduled, int actions, double utility) {
        StoreR27.EvolutionSink scores = new StoreR27.EvolutionSink(LOOP, eval.data.issues[0]);
        scores.score(first.policyHash(), eval.data.datasetHash, score(SplitR27.SELECT, 40, 40, 0.2));
        scores.score(second.policyHash(), eval.data.datasetHash, score(SplitR27.SELECT, scheduled, actions, utility));
        write(store, scores.batch);
    }

    private static long eligibleScoreRows(SQLiteDatabase db, String dataset) {
        try (Cursor rows = db.rawQuery("SELECT count(*) FROM r27_replay_score WHERE dataset_hash=? "
                + "AND split=? AND n_sched>=30 AND n_act>0 AND paired_u_lcb>0", new String[]{dataset, SplitR27.SELECT})) {
            assertTrue(rows.moveToFirst());
            return rows.getLong(0);
        }
    }

    private static void write(DbStore store, ResearchWriteQueueR27.Batch batch) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(LOOP, db -> {
            ResearchWriteQueueR27.execute(db, batch, LOOP);
            return 0;
        });
    }
}
