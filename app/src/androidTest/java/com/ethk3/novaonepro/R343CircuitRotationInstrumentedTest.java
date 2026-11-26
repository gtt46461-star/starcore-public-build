package com.ethk3.novaonepro;

import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import com.ethk3.novaonepro.strategyv2.StartupProbePolicyR343;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Protects per-dependency startup probe budgets across a persistent journal rotation. */
public final class R343CircuitRotationInstrumentedTest {
    private static final String A = "a".repeat(64);
    private static final String B = "b".repeat(64);

    private static final class Clock implements RuntimeObservationScopeR341.Clock {
        long elapsed = 1000L;
        @Override public long wall() { return 1_700_000_000_000L + elapsed; }
        @Override public long monotonic() { return elapsed; }
        void passCooldown() { elapsed += StartupProbePolicyR343.RETRY_MS; }
    }

    private static void failures(Checks checks, StartupStageJournalR341 journal, String dependency,
            String attemptPrefix) {
        for (int index = 0; index < 3; index++) {
            StartupStageJournalR341.Token stage = journal.begin("owner", attemptPrefix + index,
                    StartupStageJournalR341.Stage.OWNER_BEGIN, dependency);
            checks.require(stage.beginReceipt.durable, "Failure test persisted its open owner stage");
            checks.require(journal.recordUncaught(stage, new IllegalStateException("CIRCUIT_FIXTURE_BUG")).durable,
                    "Failure test persisted its original outcome");
        }
        checks.require(journal.hasDeterministicFailureLimit(dependency), "Repeated bugs opened the circuit");
    }

    private static Map<String, Object> latestProbeReservations(StartupStageJournalR341 journal) {
        Map<String, Object> counts = new LinkedHashMap<>();
        List<?> frames = (List<?>) journal.snapshot().get("frames");
        for (Object entry : frames) {
            Map<?, ?> frame = (Map<?, ?>) entry;
            if ("CIRCUIT_PROBE".equals(frame.get("stage")) && "END".equals(frame.get("record"))) {
                counts.put((String) frame.get("inputHash"), frame.get("probeAttempt"));
            }
        }
        return counts;
    }

    @Test public void returningDependencyCannotExceedItsPreRotationBudget() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            Clock clock = new Clock();
            RuntimeObservationScopeR341 scope = new RuntimeObservationScopeR341(A, BuildConfig.SOURCE_COMMIT_R26,
                    "probe-rotation-fixture", "test-boot", "test-db", clock.wall(), clock.monotonic(), null);
            File root = new File(app.getNoBackupFilesDir(), "r343-circuit-cross-dependency");
            StartupStageJournalR341 journal = StartupStageJournalR341.open(root, "research", clock, scope);
            failures(checks, journal, A, "first-a-");
            for (int probe = 1; probe <= StartupProbePolicyR343.MAX_PROBES_PER_DEPENDENCY; probe++) {
                clock.passCooldown();
                checks.require(journal.reserveDeterministicProbeR343(A), "A receives its allowed probe");
            }
            clock.passCooldown();
            checks.require(!journal.reserveDeterministicProbeR343(A), "A has exhausted its probe budget");
            failures(checks, journal, B, "b-");
            clock.passCooldown();
            checks.require(journal.reserveDeterministicProbeR343(B), "B has an independent probe budget");
            for (int index = 0; index < 470; index++) {
                StartupStageJournalR341.Token unrelated = journal.begin("fixture", "rotation-" + index,
                        StartupStageJournalR341.Stage.RUNTIME_BASE, null);
                checks.require(unrelated.beginReceipt.durable, "Unrelated stage began");
                checks.require(journal.end(unrelated, StartupStageJournalR341.Outcome.VERIFIED, "fixture", 0).durable,
                        "Unrelated stage ended");
            }
            StartupStageJournalR341 reopened = StartupStageJournalR341.open(root, "research", clock, scope);
            Map<String, Object> reservations = latestProbeReservations(reopened);
            checks.equal(3L, reservations.get(A), "Persisted A counter survives B and compaction");
            checks.equal(1L, reservations.get(B), "Persisted B counter survives compaction");
            failures(checks, reopened, A, "return-a-");
            clock.passCooldown();
            checks.require(!reopened.reserveDeterministicProbeR343(A), "Returning to A grants no fourth probe");
            checks.equal("PROBE_LIMIT_REACHED_DEPENDENCY_CHANGE_REQUIRED", reopened.deterministicProbeStatusR343(),
                    "Denial reports the retained budget");
            checks.noDatabase("ANDROID_ENGINEERING_STARTUP_JOURNAL",
                    "The original framed journal stores startup probes before any database opens");
            checks.record("latestReservationCounters", reservations);
            checks.record("status", reopened.deterministicProbeStatusR343());
        });
    }
}
