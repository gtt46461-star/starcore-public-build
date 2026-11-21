package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** Drive the production selector, compute permits, helpers and termination in one native SQLite fixture. */
public final class ResearchSchedulingR335Host {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Task implements ResearchPoolR27.Task {
        private final String id;
        Task(String id) { this.id = id; }
        public String id() { return id; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker) {
            return ResearchPoolR27.Outcome.progress(1, 1);
        }
    }

    private static ResearchPoolR27.Task next(ResearchPoolR27 pool) throws Exception {
        Method method = ResearchPoolR27.class.getDeclaredMethod("next");
        method.setAccessible(true);
        return (ResearchPoolR27.Task) method.invoke(pool);
    }

    @SuppressWarnings("unchecked")
    private static Map<String,PreparedTaskR30R1> prepared(ResearchPoolR27 pool) throws Exception {
        Field field = ResearchPoolR27.class.getDeclaredField("prepared");
        field.setAccessible(true);
        return (Map<String,PreparedTaskR30R1>) field.get(pool);
    }

    private static int owners() {
        return ((Number) ResearchPoolR27.computeBudgetSnapshotR31().get("count")).intValue();
    }

    private static PreparedTaskR30R1 r27(ResearchPoolR27.Worker worker, Task task) {
        return new PreparedTaskR30R1(task, worker, new RuntimeCoordinatorR26.Claim(task.id(), "scheduler-fixture", 1, 0));
    }

    private static CooperativeContinuationR26<Integer> r26(String id) {
        return new CooperativeContinuationR26<>("starcore-r26-" + id, () -> 1, () -> false, () -> {}, System::nanoTime, "scheduler-fixture");
    }

    private static void drain(PreparedTaskR30R1 task) {
        task.close();
        check(task.awaitTermination(3000), "R27_OWNER_DID_NOT_TERMINATE");
    }

    private static void drain(CooperativeContinuationR26<?> task) {
        task.close();
        check(task.awaitTermination(3000), "R26_OWNER_DID_NOT_TERMINATE");
    }

    private static void requiresWait(Runnable attempt) {
        try { attempt.run(); } catch (CooperativeContinuationR26.CapacityUnavailable expected) { return; }
        throw new AssertionError("RESERVED_OR_OCCUPIED_CONTINUATION_WAS_ADMITTED");
    }

    private static void helperIsolation(ResearchPoolR27 pool) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        List<Future<?>> work = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) work.add(pool.compute().submit(() -> { release.await(); return null; }));
            check(owners() == 6, "FOUR_CONTINUATIONS_AND_TWO_HELPERS");
            try {
                pool.compute().execute(() -> {});
                throw new AssertionError("THIRD_HELPER_WAS_ADMITTED");
            } catch (RejectedExecutionException expected) {
                check(expected.getMessage().contains("R31_COMPUTE_CAPACITY_WAIT"), "UNEXPECTED_HELPER_REJECTION");
            }
        } finally {
            release.countDown();
            for (Future<?> task : work) task.get(3, TimeUnit.SECONDS);
        }
    }

    private static void parkedOwnerAndWaitingTraining(R31OwnedRuntimeFixture fixture) throws Exception {
        try (ResearchPoolR27 pool = new ResearchPoolR27(fixture.worker.store(), "scheduler-parked", 2)) {
            ResearchPoolR27.Worker worker = pool.new Worker(fixture.worker.ro, "fixture-worker");
            Task newTask = new Task("newly-registered"), parked = new Task("r27-evolution");
            pool.register(newTask);
            pool.register(parked);
            Map<String,PreparedTaskR30R1> prepared = prepared(pool);
            List<PreparedTaskR30R1> held = new ArrayList<>();
            try {
                for (int i = 0; i < 4; i++) held.add(r27(worker, i == 0 ? parked : new Task("inflight-" + i)));
                prepared.put(parked.id(), held.get(0));
                ResearchPoolR27.Task selected = next(pool);
                System.out.println("FULL_POOL_SELECTED " + (selected == null ? "none" : selected.id()));
                check(selected == parked, "FULL_POOL_SELECTED_UNADMITTED_TASK_WHILE_PARKED_OWNER_READY");
                fixture.owned(() -> { helperIsolation(pool); return null; });
                requiresWait(() -> r26("waiting-trainer"));
                drain(held.remove(held.size() - 1));
                requiresWait(() -> r27(worker, newTask));
                CooperativeContinuationR26<Integer> admitted = r26("waiting-trainer");
                try { check(owners() == 4, "TRAINER_DID_NOT_RECEIVE_NEXT_SLOT"); }
                finally { drain(admitted); }
            } finally {
                prepared.clear();
                for (PreparedTaskR30R1 task : held) drain(task);
            }
        }
        check(owners() == 0, "UNCLEAN_R27_OR_HELPER_EXIT");
    }

    private static void waitingResearch(R31OwnedRuntimeFixture fixture, boolean waitingTaskStops) throws Exception {
        String label = waitingTaskStops ? "timeout" : "fair";
        try (ResearchPoolR27 pool = new ResearchPoolR27(fixture.worker.store(), "scheduler-" + label, 1)) {
            ResearchPoolR27.Worker worker = pool.new Worker(fixture.worker.ro, "fixture-worker");
            Task task = new Task("r27-evolution-" + label);
            pool.register(task);
            List<CooperativeContinuationR26<Integer>> held = new ArrayList<>();
            try {
                for (int i = 0; i < 4; i++) held.add(r26(label + "-" + i));
                check(next(pool) == null, "ALLOCATED_DB_CLAIM_WITHOUT_COMPUTE_CAPACITY");
                drain(held.remove(held.size() - 1));
                requiresWait(() -> r26(label + "-additional"));
                if (waitingTaskStops) {
                    Thread.sleep(2 * CooperativeContinuationR26.SLICE_MS + 150);
                    held.add(r26(label + "-after-timeout"));
                    check(owners() == 4, "STOPPED_LANE_HELD_LAST_SLOT");
                } else {
                    check(next(pool) == task, "STARVED_RESEARCH_OWNER_WAS_NOT_SELECTED");
                    PreparedTaskR30R1 admitted = r27(worker, task);
                    try { check(owners() == 4, "RESEARCH_DID_NOT_RECEIVE_NEXT_SLOT"); }
                    finally { drain(admitted); }
                }
            } finally {
                for (CooperativeContinuationR26<Integer> owner : held) drain(owner);
            }
        }
        check(owners() == 0, "UNCLEAN_R26_EXIT");
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path root = Files.createTempDirectory("r335-scheduler-");
        Files.createDirectories(root.resolve("databases"));
        Files.createDirectories(root.resolve("files"));
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + root.resolve("databases/main.db"));
             R31OwnedRuntimeFixture fixture = new R31OwnedRuntimeFixture(new SQLiteDatabase(db), "r27-evolution")) {
            parkedOwnerAndWaitingTraining(fixture);
            waitingResearch(fixture, false);
            waitingResearch(fixture, true);
        }
        System.out.println("PARKED_OWNER_RESUMED BOTH_LANES_ADMITTED STOPPED_LANE_EXPIRED FOUR_PLUS_TWO_VERIFIED");
    }
}
