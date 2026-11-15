package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.AppConfig;
import com.ethk3.novaonepro.R30FaultAndroidRunner;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * ART protocol tests, using production models and the real default per-process memory budget.
 * The 160/161 legal-permutation rows are SYNTHETIC_ENGINEERING_ONLY, not REAL, FUTURE,
 * independent quality samples, qualification evidence, or a 24/72-hour endurance result.
 * All ten positions and all historical output cells are compared, including the first-row NaNs.
 * No probe, model, clock, compute-permit provider, or garbage collector is replaced here.
 */
@RunWith(AndroidJUnit4.class)
public final class R31FamilyContinuationInstrumentedTest {
    private static final String TAG = "R31FamilyART";
    private static final String SCOPE = "SYNTHETIC_ENGINEERING_ONLY";
    private static final long METHOD_TIMEOUT_MS = 55_000;
    private static final long WORK_WINDOW_MS = 45_000;
    private static final long JOIN_MS = 2_000;

    /** Refuse the ordinary application runner before creating any production cache or worker. */
    private static void requireSandbox() throws Exception {
        assertTrue("R30FaultAndroidRunner is required before touching application resources",
                InstrumentationRegistry.getInstrumentation() instanceof R30FaultAndroidRunner);
        String sandbox = InstrumentationRegistry.getArguments().getString("r30Sandbox");
        assertNotNull("An explicit runner sandbox argument is required", sandbox);
        assertTrue("Invalid runner sandbox", sandbox.matches("[A-Za-z0-9_-]{1,80}"));
        Context app = ApplicationProvider.getApplicationContext();
        String expected = "/r30-instrumentation/" + sandbox + "/";
        assertTrue("The database path must belong to the runner sandbox",
                app.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath().contains(expected));
        assertTrue("The files path must belong to the same runner sandbox",
                app.getFilesDir().getCanonicalPath().contains(expected));
        assertEquals("The production action contract remains fixed K=3", 3, AppConfig.FIXED_K);
        assertNull("A previous test leaked an execution token onto its caller", ExecutionBudgetR26.current());
    }

    private static void requireEngineeringRows(ReplayDatasetR27 data) {
        assertTrue("Fixture scope is 120-250 engineering rows", data.size() >= 120 && data.size() <= 250);
        assertEquals("No invalid or duplicate fixture row may disappear", 0, data.rejectedRows);
        for (int row = 0; row < data.size(); row++) {
            assertTrue("Synthetic issue label, never a real issue", data.issues[row].startsWith("S"));
            assertTrue("Every input is a legal ten-number permutation", ReplayDatasetR27.legal(data.draws[row]));
            if (row > 0) {
                assertEquals("Keep chronological point-in-time order", data.ordinals[row - 1] + 1, data.ordinals[row]);
                assertTrue("This fixture does not invent missing issues", data.consecutive(row, 1));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static FeatureCacheR27.FamilyStream stream(FeatureCacheR27 cache, int family) throws Exception {
        Field field = FeatureCacheR27.class.getDeclaredField("streams");
        field.setAccessible(true);
        FeatureCacheR27.FamilyStream result =
                ((Map<Integer, FeatureCacheR27.FamilyStream>) field.get(cache)).get(family);
        assertNotNull("The production stream must actually exist: F" + family, result);
        return result;
    }

    private static double[] pending(FeatureCacheR27.FamilyStream stream) throws Exception {
        Field field = FeatureCacheR27.FamilyStream.class.getDeclaredField("pendingValuesR31");
        field.setAccessible(true);
        return (double[]) field.get(stream);
    }

    private static byte[] codec(FeatureCacheR27.FamilyStream stream) throws Exception {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            stream.model.write(output);
            output.flush();
            return bytes.toByteArray();
        }
    }

    private static void bits(String label, double[] expected, double[] actual) {
        assertEquals(label + " length", expected.length, actual.length);
        for (int cell = 0; cell < expected.length; cell++) {
            assertEquals(label + " raw double bits at cell " + cell,
                    Double.doubleToRawLongBits(expected[cell]), Double.doubleToRawLongBits(actual[cell]));
        }
    }

    private static void within(long deadline) {
        assertTrue("Engineering protocol test exceeded its real monotonic work window",
                SystemClock.elapsedRealtime() < deadline);
    }

    private static void stopped(CooperativeContinuationR26<?> work) {
        // A JUnit timeout may interrupt its caller. Still request cancellation and observe a bounded
        // actual join before restoring that flag; cancellation itself never proves resource release.
        boolean interrupted = Thread.interrupted();
        try {
            work.close();
            assertTrue("Actual production continuation thread exit must be observed", work.awaitTermination(JOIN_MS));
            assertTrue("A result-ready flag is insufficient", work.isTerminated());
            assertEquals(CooperativeContinuationR26.State.TERMINATED, work.state());
            assertEquals("BODY_EXITED", work.snapshot().phase);
            assertEquals("The real permit reaper must not report cleanup failure", "", work.snapshot().cleanupFailure);
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static final class Attempt {
        final boolean yielded;
        final double[] result;
        Attempt(boolean yielded, double[] result) { this.yielded = yielded; this.result = result; }
    }

    /**
     * The model first exhausts a real operation token. The still-live body then parks at the
     * production continuation boundary so the test can cancel it and join it before retrying.
     * This boundary never alters model state and is not a substituted model/probe or clock.
     */
    private static Attempt attempt(FeatureCacheR27 cache, int family, long operations,
                                   String owner, long deadline) throws Exception {
        within(deadline);
        AtomicReference<BudgetTokenSU1.Yield> exhausted = new AtomicReference<>();
        AtomicReference<BudgetTokenSU1> token = new AtomicReference<>();
        AtomicBoolean bodyExited = new AtomicBoolean();
        AtomicBoolean tokenCleared = new AtomicBoolean();
        AtomicLong bodyThread = new AtomicLong(-1);
        long callerThread = Thread.currentThread().getId();
        CooperativeContinuationR26<double[]> work = new CooperativeContinuationR26<>(
                "R31_ART_FAMILY_" + family, () -> {
                    bodyThread.set(Thread.currentThread().getId());
                    try {
                        assertTrue("The real continuation installs its worker context", CooperativeContinuationR26.installed());
                        BudgetTokenSU1 limited = new BudgetTokenSU1(30_000, operations, () -> false);
                        token.set(limited);
                        ExecutionBudgetR26.enter(limited);
                        try {
                            return cache.family(1, family);
                        } catch (BudgetTokenSU1.Yield actual) {
                            exhausted.set(actual);
                            ExecutionBudgetR26.leave();
                            CooperativeContinuationR26.yieldPreparedWrite();
                            throw new AssertionError("An exhausted attempt must be cancelled, never resumed concurrently");
                        } finally {
                            ExecutionBudgetR26.leave();
                            tokenCleared.set(ExecutionBudgetR26.current() == null);
                        }
                    } finally {
                        bodyExited.set(true);
                    }
                }, () -> false, null, Debug::threadCpuTimeNanos, owner);
        CooperativeContinuationR26.Slice<double[]> observed = null;
        try {
            do {
                within(deadline);
                observed = work.slice(CooperativeContinuationR26.CONTINUATION_SLICE_MS);
            } while (!observed.done && exhausted.get() == null);
            if (exhausted.get() != null) {
                assertEquals("The actual operation budget must cause the yield", "SU1_BUDGET_EXHAUSTED",
                        exhausted.get().getMessage());
                assertFalse("The old computation must still be alive when cancellation is requested", bodyExited.get());
                assertFalse(work.isTerminated());
                assertEquals(CooperativeContinuationR26.State.PAUSED, work.state());
            } else if (observed.error != null) {
                throw new AssertionError("Real ART model/admission failed: "
                        + MemoryBudgetR31.processBudget().snapshot(), observed.error);
            }
        } finally {
            stopped(work);
        }
        assertTrue("The original body finally block must have executed", bodyExited.get());
        assertTrue("The original body must clear its execution token", tokenCleared.get());
        assertTrue("The computation must have run on its real worker", bodyThread.get() >= 0 && bodyThread.get() != callerThread);
        assertNull("No execution token may escape to the caller", ExecutionBudgetR26.current());
        if (exhausted.get() != null) {
            Field remaining = BudgetTokenSU1.class.getDeclaredField("remainingOperations");
            remaining.setAccessible(true);
            assertTrue("Prove operation exhaustion rather than relying on a wall-time deadline",
                    remaining.getLong(token.get()) < 0);
            return new Attempt(true, null);
        }
        assertNotNull("A completed production call must return its whole output", observed.result);
        return new Attempt(false, observed.result);
    }

    private static void allPositions(FeatureCacheR27 actual, FeatureCacheR27 fresh,
                                     int family, long deadline) throws Exception {
        int rows = actual.data.size();
        assertEquals(rows, fresh.data.size());
        assertSame("Use the real default process budget", MemoryBudgetR31.processBudget(), actual.memoryBudgetR31());
        for (int position = 1; position <= 10; position++) {
            within(deadline);
            double[] expected = fresh.family(position, family);
            double[] scores = actual.family(position, family);
            assertEquals("Every row and all ten numbers must remain in scope", rows * 10, scores.length);
            bits("F" + family + " position " + position, expected, scores);
            double[] rowScores = new double[10];
            int[] order = new int[10];
            for (int row = 0; row < rows; row++) {
                for (int number = 0; number < 10; number++) {
                    double score = scores[row * 10 + number];
                    if (row == 0) assertTrue("No pre-history prediction is invented", Double.isNaN(score));
                    else assertTrue("Complete predictions must be finite", Double.isFinite(score));
                    rowScores[number] = score;
                }
                if (row > 0) {
                    ReplayEvaluatorR27.top3(rowScores, order);
                    int mask = 0;
                    for (int choice = 0; choice < AppConfig.FIXED_K; choice++) mask |= 1 << order[choice];
                    assertEquals("Keep the actual stable fixed-K3 action head", 3, Integer.bitCount(mask));
                }
            }
        }
        FeatureCacheR27.FamilyStream complete = stream(actual, family);
        assertEquals("The last row is predicted before its label can be fed", rows - 1, complete.fed);
        assertEquals("No future/current final-row label may enter the checkpoint",
                actual.data.ordinals[rows - 2], complete.lastFedOrdinal);
        assertArrayEquals("The production codec, including optimizer/context, must match fresh",
                codec(stream(fresh, family)), codec(complete));
    }

    @Test(timeout = METHOD_TIMEOUT_MS)
    public void realOperationYieldsCancelAndJoinOldBodiesThenResumeTheSameStream() throws Exception {
        requireSandbox();
        long deadline = SystemClock.elapsedRealtime() + WORK_WINDOW_MS;
        String owner = "R31_ART_SYNTHETIC_RESUME_" + UUID.randomUUID();
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(160, 3_215, null);
        requireEngineeringRows(data);
        String inputHash = data.datasetHash;
        int yields = 0;
        try (FeatureCacheR27 resumed = new FeatureCacheR27(data);
             FeatureCacheR27 fresh = new FeatureCacheR27(data)) {
            assertSame(MemoryBudgetR31.processBudget(), resumed.memoryBudgetR31());
            FeatureCacheR27.FamilyStream original = null;
            int previous = -1;
            double[] completed = null;
            for (int turn = 0; turn < data.size() && completed == null; turn++) {
                Attempt attempt = attempt(resumed, 12, 7, owner, deadline);
                FeatureCacheR27.FamilyStream current = stream(resumed, 12);
                if (original == null) original = current;
                assertSame("Cancellation must resume the same production stream, never rebuild it", original, current);
                if (attempt.yielded) {
                    yields++;
                    assertTrue("Each bounded attempt must retain actual committed-row progress", current.fed > previous);
                    assertTrue("A yielded body cannot claim completion", current.fed < data.size() - 1);
                    assertEquals("Partial output must stay private", 0, current.values.length);
                    previous = current.fed;
                } else completed = attempt.result;
            }
            assertNotNull("Bounded real-token retries must complete", completed);
            assertTrue("Repeated recovery must actually be exercised", yields >= 10);
            bits("Returned resumed output", fresh.family(1, 12), completed);
            allPositions(resumed, fresh, 12, deadline);
            assertEquals("The chronological fixture is immutable", inputHash, data.datasetHash);
            Log.i(TAG, SCOPE + " resume rows=160 family=F12 positions=10 actualYields=" + yields
                    + " oldBodies=JOINED comparison=ALL_RAW_DOUBLE_BITS_AND_MODEL_CODEC");
        } finally {
            assertTrue("No cancelled body may outlive its cache owner", CooperativeContinuationR26.awaitOwnerStopped(owner, JOIN_MS));
        }
    }

    private static final class ParentState {
        final FeatureCacheR27.FamilyStream identity;
        final byte[] model;
        final int fed;
        final long lastOrdinal;
        final int[] lastDraw;
        final double[] privateRows;
        ParentState(FeatureCacheR27.FamilyStream stream) throws Exception {
            identity = stream;
            model = codec(stream);
            fed = stream.fed;
            lastOrdinal = stream.lastFedOrdinal;
            lastDraw = stream.lastFedDraw.clone();
            assertNotNull("Partial output must have an actual private backing", pending(stream));
            privateRows = pending(stream).clone();
        }
        void unchanged(FeatureCacheR27 cache, int family) throws Exception {
            FeatureCacheR27.FamilyStream current = stream(cache, family);
            assertSame(identity, current);
            assertEquals("Child cannot feed its parent", fed, current.fed);
            assertEquals(lastOrdinal, current.lastFedOrdinal);
            assertArrayEquals(lastDraw, current.lastFedDraw);
            assertEquals("Parent remains partial until it is explicitly resumed", 0, current.values.length);
            assertArrayEquals("Child updates cannot mutate parent model/optimizer/context", model, codec(current));
            bits("Parent private pending rows", privateRows, pending(current));
        }
    }

    @Test(timeout = METHOD_TIMEOUT_MS)
    public void partialParentsDeepForkAcrossFamiliesAndAllTenPositionsMatchFresh() throws Exception {
        requireSandbox();
        long deadline = SystemClock.elapsedRealtime() + WORK_WINDOW_MS;
        String owner = "R31_ART_SYNTHETIC_FORK_" + UUID.randomUUID();
        ReplayDatasetR27 source = ReplayDatasetR27.synthetic(161, 3_216, null);
        List<ReplayDatasetR27.Row> prefix = new ArrayList<>();
        for (int row = 0; row < 160; row++)
            prefix.add(new ReplayDatasetR27.Row(source.issues[row], source.ordinals[row], source.draws[row]));
        ReplayDatasetR27 old = ReplayDatasetR27.of(prefix);
        ReplayDatasetR27 appended = old.append(new ReplayDatasetR27.Row(
                source.issues[160], source.ordinals[160], source.draws[160]));
        requireEngineeringRows(old);
        requireEngineeringRows(appended);
        assertEquals("Exercise the real append API without changing the prefix", source.datasetHash, appended.datasetHash);
        int[] families = {1, 3, 12};
        ParentState[] before = new ParentState[families.length];
        try (FeatureCacheR27 parent = new FeatureCacheR27(old);
             FeatureCacheR27 child = new FeatureCacheR27(appended);
             FeatureCacheR27 freshOld = new FeatureCacheR27(old);
             FeatureCacheR27 freshFull = new FeatureCacheR27(appended)) {
            for (int index = 0; index < families.length; index++) {
                int family = families[index];
                // Exact-DP/tape kernels consume more real checkpoints per row than F12.
                // These finite token counts allow rows to commit, but cannot finish 160 rows.
                Attempt partial = attempt(parent, family, family == 12 ? 7 : 1_024, owner, deadline);
                assertTrue("Every selected parent must genuinely yield before the deep fork", partial.yielded);
                FeatureCacheR27.FamilyStream current = stream(parent, family);
                assertTrue("At least one causal training row must really commit", current.fed > 0 && current.fed < old.size() - 1);
                assertEquals(0, current.values.length);
                before[index] = new ParentState(current);
            }
            child.extendFrom(parent);
            for (int index = 0; index < families.length; index++) {
                int family = families[index];
                FeatureCacheR27.FamilyStream original = stream(parent, family);
                FeatureCacheR27.FamilyStream fork = stream(child, family);
                assertNotSame("The child must own a distinct stream", original, fork);
                assertNotSame("Mutable model and optimizer state cannot be shared", original.model, fork.model);
                assertNotSame("Pending output must be a real private deep copy", pending(original), pending(fork));
                bits("Copied partial prefix", pending(original), pending(fork));
                assertArrayEquals(codec(original), codec(fork));
                assertNotSame(original.st, fork.st);
                assertNotSame(original.lastFedDraw, fork.lastFedDraw);
                for (int position = 0; position < 10; position++) assertNotSame(original.p[position], fork.p[position]);
            }
            for (int index = 0; index < families.length; index++) {
                allPositions(child, freshFull, families[index], deadline);
                // Check every still-partial parent after each child's advancement, before allowing
                // any parent to progress. A copied ID or aliased model cannot satisfy these checks.
                for (int other = 0; other < families.length; other++) before[other].unchanged(parent, families[other]);
            }
            for (int family : families) allPositions(parent, freshOld, family, deadline);
            Log.i(TAG, SCOPE + " deepFork parentRows=160 childRows=161 families="
                    + Arrays.toString(families) + " positions=10 parentIsolation=CODEC_AND_PENDING_ROWS"
                    + " comparison=ALL_RAW_DOUBLE_BITS_AND_MODEL_CODEC");
        } finally {
            assertTrue("No old worker may survive partial-parent recovery", CooperativeContinuationR26.awaitOwnerStopped(owner, JOIN_MS));
        }
    }
}
