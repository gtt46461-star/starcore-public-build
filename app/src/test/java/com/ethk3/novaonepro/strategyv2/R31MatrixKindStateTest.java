package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B08-C01#A1-#A4: per-kind matrix state with isolated failures and dependency keys. */
public final class R31MatrixKindStateTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    static final class Owned implements AutoCloseable {
        final FeatureCacheR27 cache; final EvalContextR27 ctx;
        Owned() {
            ReplayDatasetR27 synthetic = ReplayDatasetR27.synthetic(1600, 91, new int[0]); List<ReplayDatasetR27.Row> rows = new ArrayList<>();
            long start = com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202610240001");
            for (int i = 0; i < synthetic.size(); i++) rows.add(new ReplayDatasetR27.Row(com.ethk3.novaonepro.TargetIssueResolver.fromSequenceOrdinalSU1(start + i), start + i, synthetic.draws[i]));
            ReplayDatasetR27 d = ReplayDatasetR27.of(rows); cache = new FeatureCacheR27(d); ctx = new EvalContextR27(cache, SplitR27.of(d, null), null);
        }
        @Override public void close() { try { ctx.close(); } finally { cache.close(); } }
    }
    static PolicySpecR27 student() { return new PolicySpecR27(PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32)), 1, PolicySpecR27.Rule.ALWAYS, 0, 1, 1, false, .01, 0, IdeaFamilyR27.meta("I09", "TEST", 0, 1, "HOST_SYNTHETIC")); }
    static MatrixResearchR30R1.OutcomeR31 produce(Owned o, String kind, PolicySpecR27 policy) throws Exception {
        return MatrixResearchR30R1.produceR31(kind, policy != null, o.ctx.data.datasetHash, 3, () -> MatrixResearchR30R1.build(kind, o.ctx, MatrixResearchR30R1.SOURCE_ONLY_KINDS_R31.contains(kind) ? null : policy, 3));
    }

    @Test public void missingStudentWaitsWhileSourceOnlyMatricesStillProduce() throws Exception {
        try (Owned o = new Owned()) {
            for (String kind : MatrixResearchR30R1.STUDENT_KINDS_R31) {
                MatrixResearchR30R1.OutcomeR31 waiting = produce(o, kind, null);
                assertEquals(kind, MatrixResearchR30R1.StatusR31.WAITING_STUDENT, waiting.status); assertNull(waiting.tensor);
                assertEquals(MatrixResearchR30R1.StatusR31.PRODUCED, produce(o, kind, student()).status);
            }
            for (String kind : Arrays.asList("NUMBER_POSITION_COMPATIBILITY", "CROSS_POSITION_FLOW")) {
                MatrixResearchR30R1.OutcomeR31 produced = produce(o, kind, null);
                assertEquals(kind, MatrixResearchR30R1.StatusR31.PRODUCED, produced.status);
                assertTrue("association is labelled, never an intervention", produced.tensor.payload.contains("OBSERVATIONAL_ASSOCIATION_NOT_INTERVENTION"));
                assertTrue("a tensor grants no main-chain permission", produced.tensor.payload.contains("\"mainChainPermissionR31\":false"));
            }
            for (MatrixRegistryR30.Definition d : MatrixRegistryR30.all()) {
                assertFalse(d.id, d.dependencyR31.isEmpty()); assertFalse(d.id, d.waitRuleR31.isEmpty()); assertEquals(d.dependencyR31, d.json().get("dependency"));
                if (MatrixResearchR30R1.SOURCE_ONLY_KINDS_R31.contains(d.kind)) assertEquals("NEVER_WAITS_FOR_STUDENT", d.waitRuleR31);
                if (MatrixResearchR30R1.STUDENT_KINDS_R31.contains(d.kind)) assertEquals("STUDENT", d.dependencyR31);
            }
        }
    }

    @Test public void unchangedDependencyIsDecidedOnceAndAStudentPublicationReopensIt() throws Exception {
        try (Owned o = new Owned()) {
            String noStudent = MatrixResearchR30R1.dependencyKeyR31(o.ctx.data.datasetHash, 3, "STUDENT_ERROR", StatsR27.sha256(""), 0, "c");
            Map<String, Object> receipt = MatrixResearchR30R1.receiptR31("R930:STUDENT_ERROR", noStudent, o.ctx.data.datasetHash, 3, produce(o, "STUDENT_ERROR", null), null, 1);
            assertEquals("WAITING_STUDENT", receipt.get("statusR31"));
            Map<?, ?> stored = (Map<?, ?>) JsonR27.parse(JsonR27.write(receipt)); int rescheduled = 0;
            for (int i = 0; i < 100; i++) if (!MatrixResearchR30R1.decidedR31(stored, MatrixResearchR30R1.dependencyKeyR31(o.ctx.data.datasetHash, 3, "STUDENT_ERROR", StatsR27.sha256(""), 0, "c"))) rescheduled++;
            assertEquals("100 schedules with an unchanged dependency create no new waiting job", 0, rescheduled);
            String published = MatrixResearchR30R1.dependencyKeyR31(o.ctx.data.datasetHash, 3, "STUDENT_ERROR", StatsR27.sha256(student().policyHash()), 0, "c");
            assertFalse("a published student changes the dependency key", MatrixResearchR30R1.decidedR31(stored, published));
            assertEquals(MatrixResearchR30R1.StatusR31.PRODUCED, produce(o, "STUDENT_ERROR", student()).status);
            for (String changed : new String[]{MatrixResearchR30R1.dependencyKeyR31("x", 3, "STUDENT_ERROR", StatsR27.sha256(""), 0, "c"), MatrixResearchR30R1.dependencyKeyR31(o.ctx.data.datasetHash, 4, "STUDENT_ERROR", StatsR27.sha256(""), 0, "c"),
                    MatrixResearchR30R1.dependencyKeyR31(o.ctx.data.datasetHash, 3, "STUDENT_ERROR", StatsR27.sha256(""), 9, "c")})
                assertFalse("dataset, revision or measured version reopen the kind", MatrixResearchR30R1.decidedR31(stored, changed));
        }
    }

    @Test public void invalidValuesNeverReplaceTheLastValidTensor() throws Exception {
        try (Owned o = new Owned()) {
            MatrixResearchR30R1.OutcomeR31 valid = produce(o, "NUMBER_POSITION_COMPATIBILITY", null);
            Map<String, Object> first = MatrixResearchR30R1.receiptR31("R930:NUMBER_POSITION_COMPATIBILITY", "k1", o.ctx.data.datasetHash, 3, valid, null, 1);
            MatrixTensorR30R1.Tensor good = valid.tensor; String through = good.throughIssue, target = good.targetIssue;
            List<String> labels = Arrays.asList("a", "b");
            MatrixResearchR30R1.OutcomeR31 nan = MatrixResearchR30R1.produceR31("NUMBER_POSITION_COMPATIBILITY", true, o.ctx.data.datasetHash, 3, () -> new MatrixTensorR30R1.Tensor("NUMBER_POSITION_COMPATIBILITY", "SELECT", o.ctx.data.datasetHash, StatsR27.sha256("SOURCE_ONLY"), through, target, 3, labels, labels, new double[][]{{Double.NaN, 1}, {1, 1}}, new long[][]{{1, 1}, {1, 1}}, new LinkedHashMap<>()));
            MatrixResearchR30R1.OutcomeR31 dims = MatrixResearchR30R1.produceR31("NUMBER_POSITION_COMPATIBILITY", true, o.ctx.data.datasetHash, 3, () -> new MatrixTensorR30R1.Tensor("NUMBER_POSITION_COMPATIBILITY", "SELECT", o.ctx.data.datasetHash, StatsR27.sha256("SOURCE_ONLY"), through, target, 3, labels, labels, new double[][]{{0, 1}}, new long[][]{{1, 1}}, new LinkedHashMap<>()));
            MatrixResearchR30R1.OutcomeR31 zeros = MatrixResearchR30R1.produceR31("NUMBER_POSITION_COMPATIBILITY", true, o.ctx.data.datasetHash, 3, () -> new MatrixTensorR30R1.Tensor("NUMBER_POSITION_COMPATIBILITY", "SELECT", o.ctx.data.datasetHash, StatsR27.sha256("SOURCE_ONLY"), through, target, 3, labels, labels, new double[][]{{0, 0}, {0, 0}}, new long[][]{{0, 0}, {0, 0}}, new LinkedHashMap<>()));
            MatrixResearchR30R1.OutcomeR31 revised = MatrixResearchR30R1.produceR31("NUMBER_POSITION_COMPATIBILITY", true, o.ctx.data.datasetHash, 4, () -> good);
            assertEquals(MatrixResearchR30R1.StatusR31.INVALID, nan.status); assertEquals(MatrixResearchR30R1.StatusR31.INVALID, dims.status);
            assertEquals(MatrixResearchR30R1.StatusR31.INSUFFICIENT, zeros.status); assertEquals("MATRIX_NO_SUPPORTED_CELL", zeros.reason);
            assertEquals(MatrixResearchR30R1.StatusR31.INVALID, revised.status); assertEquals("MATRIX_SOURCE_IDENTITY_CHANGED", revised.reason);
            Map<?, ?> previous = (Map<?, ?>) JsonR27.parse(JsonR27.write(first));
            for (MatrixResearchR30R1.OutcomeR31 bad : Arrays.asList(nan, dims, zeros, revised)) {
                assertNull("no tensor enters the success table", bad.tensor);
                Map<String, Object> r = MatrixResearchR30R1.receiptR31("R930:NUMBER_POSITION_COMPATIBILITY", "k2", o.ctx.data.datasetHash, 3, bad, previous, 1);
                assertNull(r.get("matrixHash")); assertEquals("the last valid tensor is kept", good.hash, r.get("lastValidMatrixHash")); assertEquals(true, r.get("lastValidIsStale"));
                previous = (Map<?, ?>) JsonR27.parse(JsonR27.write(r)); // survives repeated failures
            }
        }
    }

    @Test public void aFaultIsolatedToOneKindDoesNotStopTheRoundButStorageFaultsStillEscalate() throws Exception {
        try (Owned o = new Owned()) {
            Map<String, MatrixResearchR30R1.StatusR31> round = new LinkedHashMap<>();
            for (String kind : Arrays.asList("NUMBER_POSITION_COMPATIBILITY", "STUDENT_ERROR", "CROSS_POSITION_FLOW")) {
                MatrixResearchR30R1.ProducerR31 p = kind.equals("STUDENT_ERROR") ? () -> { throw new IllegalStateException("INJECTED_KIND_FAULT"); } : () -> MatrixResearchR30R1.build(kind, o.ctx, null, 3);
                round.put(kind, MatrixResearchR30R1.produceR31(kind, true, o.ctx.data.datasetHash, 3, p).status);
            }
            assertEquals(MatrixResearchR30R1.StatusR31.INVALID, round.get("STUDENT_ERROR"));
            assertEquals("the kinds after the fault still produce", MatrixResearchR30R1.StatusR31.PRODUCED, round.get("CROSS_POSITION_FLOW"));
            assertEquals(MatrixResearchR30R1.StatusR31.PRODUCED, round.get("NUMBER_POSITION_COMPATIBILITY"));
            try {
                MatrixResearchR30R1.produceR31("CROSS_POSITION_FLOW", true, o.ctx.data.datasetHash, 3, () -> { throw new IllegalStateException("WRAPPED", new java.sql.SQLException("database disk image is malformed")); });
                fail("database integrity fault was swallowed as a kind state");
            } catch (IllegalStateException expected) { assertEquals("WRAPPED", expected.getMessage()); }
            assertEquals(MatrixResearchR30R1.StatusR31.WAITING_SOURCE, MatrixResearchR30R1.produceR31("CONTRADICTION", true, o.ctx.data.datasetHash, 3, () -> null).status);
        }
    }
}
