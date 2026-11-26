package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DatabaseCoordinator.WriteClass;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteContext;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.R31FixtureSchemaOwners;
import com.ethk3.novaonepro.RuntimeObservationScopeR341;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.strategyv2.EventFrameBuilderSU1;
import com.ethk3.novaonepro.strategyv2.EventFrameSU1;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/** Disposable Android writer and frozen-history readers. The draw inputs are fixed engineering fixtures. */
public final class R343HistoryContinuityInstrumentedTest {
    private static final int ROWS = 480;
    private static final int MINIMUM = 20;
    private static final long MINUTE = 60_000L;
    private static final int PAUSE_AFTER_INDEX = 30;
    private static final long FIRST = TargetIssueResolver.sequenceOrdinalSU1("202611241375");
    private static final String TARGET = issue(ROWS);

    private static String issue(int index) {
        return TargetIssueResolver.fromSequenceOrdinalSU1(FIRST + index);
    }

    private static List<Models.Draw> history() {
        long latest = System.currentTimeMillis() - 1_000L;
        List<Models.Draw> draws = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = issue(i);
            draw.openTime = latest - (ROWS - i - 1) * MINUTE;
            if (i <= PAUSE_AFTER_INDEX) draw.openTime -= 3 * MINUTE;
            draw.fetchTime = draw.openTime + 200L;
            for (int pos = 0; pos < 10; pos++) draw.numbers[pos] = ((pos + i) % 10) + 1;
            draws.add(draw);
        }
        return draws;
    }

    private static List<Models.Draw> saveExcept(Checks checks, DbStore store, List<Models.Draw> source,
            Set<Integer> holes) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                WriteContext.of(WriteClass.MIGRATION, "R343_TEST_SU1_HISTORY_OWNER"), writer -> {
                    R31FixtureSchemaOwners.installSu1WithinWriterR31(writer);
                    return null;
                });
        List<Models.Draw> available = new ArrayList<>();
        for (int index = 0; index < source.size(); index++) {
            if (!holes.contains(index)) available.add(source.get(index));
        }
        checks.equal(available.size(), store.saveDraws(available), "Normal history writer commits only its inputs");
        List<Models.Draw> persisted = store.getDrawsBeforeIssue(TARGET, 5000);
        checks.equal(available.size(), persisted.size(), "Readback agrees with actual persisted inputs");
        for (int missing : holes) {
            checks.equal(null, store.getDrawByIssue(issue(missing)), "No label invented for " + issue(missing));
        }
        return persisted;
    }

    private static void compareSuffix(Checks checks, PITSnapshotContractR952 pin,
            EventFrameSU1 frame, List<Models.Draw> training) {
        checks.equal(pin.history.size(), frame.size(), "PIT pin and actual frame use identical eligible history");
        checks.equal(pin.history.size(), training.size(), "Training consumes the same continuous suffix");
        int[][] seen = frame.history();
        for (int row = 0; row < frame.size(); row++) {
            Models.Draw source = pin.history.get(pin.history.size() - row - 1);
            checks.equal(source.issue, training.get(row).issue, "The prepared and training targets agree");
            checks.equal(Long.parseLong(source.issue), frame.issue(row), "PIT frame identity at " + row);
            checks.equal(source.fetchTime, frame.availableAt(row), "Original eligible timestamp at " + row);
            for (int position = 0; position < 10; position++) {
                checks.equal(source.numbers[position], seen[row][position], "Frozen position is the persisted draw");
            }
        }
    }

    private static void scope(Checks checks, DbStore store, long cutoff) throws Exception {
        String database = RuntimeObservationScopeR341.databaseIdentity(store.readable());
        checks.require(database.matches("[a-f0-9-]{36}"), "Database identity resolves");
        checks.database("ANDROID_ENGINEERING_HISTORY_FIXTURE", TARGET, cutoff, database);
    }

    @Test public void passesUtcRolloverAndContiguousIdsAcrossAnExtendedInterval() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                List<Models.Draw> persisted = saveExcept(checks, store, history(), Set.of(12));
                PITSnapshotContractR952 pin = PITSnapshotContractR952.capture(store, TARGET, 5000).newestContiguous();
                pin.requireConsecutive();
                EventFrameSU1 frame = EventFrameBuilderSU1.capture(store, TARGET);
                List<Models.Draw> training = DevelopmentProductionCP12.selectHistoryCP12(persisted, MINIMUM, false);
                compareSuffix(checks, pin, frame, training);
                checks.equal(ROWS - 13, frame.size(), "Prefix before the unknown gap is excluded");
                checks.equal(issue(13), pin.firstIssue, "PIT starts after the missing issue");
                checks.equal("202611241440", issue(65), "Fixture reaches the final UTC issue");
                checks.equal("202611250001", issue(66), "Fixture crosses into the next UTC date");
                checks.require(TargetIssueResolver.exact(issue(65), issue(66)).valid,
                        "The real resolver accepts UTC rollover");
                checks.equal(4 * MINUTE, store.getDrawByIssue(issue(PAUSE_AFTER_INDEX + 1)).openTime
                        - store.getDrawByIssue(issue(PAUSE_AFTER_INDEX)).openTime,
                        "The same issue sequence accepts the engineered extended wall-time interval");
                checks.equal(null, store.getDrawByIssue(TARGET), "The fixture target remains pre-draw");
                scope(checks, store, frame.cutoff);
                checks.record("history", Map.of("target", TARGET, "first", pin.firstIssue, "last", pin.lastIssue,
                        "ignoredMissing", issue(12), "eligibleRows", frame.size(), "pitSha256", pin.snapshotHash,
                        "frameSha256", frame.contentHash, "sourceGeneration", frame.sourceGeneration,
                        "intervalScope", "CONTIGUOUS_IDS_IN_ENGINEERED_TIME_GAP"));
            } finally {
                store.close();
            }
        });
    }

    @Test public void missingDrawsOnlyExposeEligibleSuffixAndLeaveTrainingWaiting() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                List<Models.Draw> fixture = history();
                List<Models.Draw> missing = saveExcept(checks, store, fixture, Set.of(350, 435));
                EventFrameSU1 frame = EventFrameBuilderSU1.capture(store, TARGET);
                checks.equal(44, frame.size(), "Newest frame starts after the later gap");
                checks.equal(Long.parseLong(issue(436)), frame.issue(0), "No frame crosses the later gap");
                String both = waitForHistory(checks, missing, 44, issue(434), issue(436));
                checks.equal(1, store.saveDraws(List.of(fixture.get(435))), "One genuine fixture row is now delivered");
                List<Models.Draw> partial = store.getDrawsBeforeIssue(TARGET, 5000);
                String one = waitForHistory(checks, partial, 129, issue(349), issue(351));
                checks.equal(null, store.getDrawByIssue(issue(350)), "A different missing label is still absent");
                checks.equal(null, store.getDrawByIssue(TARGET), "No prior-gap operation invents the target label");
                checks.require(!EventFrameBuilderSU1.unchanged(store, frame),
                        "The prior live frame cannot pass source-generation CAS after backfill");
                scope(checks, store, frame.cutoff);
                checks.record("missingLabel", Map.of("issue", issue(350), "oldSuffix", 44, "newSuffix", 129,
                        "twoGaps", both, "oneGap", one, "frameSha256", frame.contentHash));
            } finally {
                store.close();
            }
        });
    }

    private static String waitForHistory(Checks checks, List<Models.Draw> persisted, int count,
            String breakBefore, String breakAfter) {
        try {
            DevelopmentProductionCP12.selectHistoryCP12(persisted, MINIMUM, false);
            throw new AssertionError("A gapped or undersized history entered candidate training");
        } catch (DevelopmentProductionCP12.Wait wait) {
            checks.equal("WAIT_HISTORY", wait.reason, "Candidate is held for insufficient contiguous history");
            String detail = wait.getCause().getMessage();
            checks.require(detail.contains("HISTORY_REQUIRED=441|HISTORY_ACTUAL=" + count), "Exact suffix count retained");
            checks.require(detail.contains("BREAK=" + breakBefore + "/" + breakAfter), "Exact gap retained");
            return detail;
        }
    }

    @Test public void verifiedBackfillAdvancesSourceWithoutChangingFrozenReplay() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                List<Models.Draw> fixture = history();
                List<Models.Draw> persisted = saveExcept(checks, store, fixture, Set.of(350));
                String held = waitForHistory(checks, persisted, 129, issue(349), issue(351));
                PITSnapshotContractR952 pinned = PITSnapshotContractR952.capture(store, TARGET, 5000).newestContiguous();
                EventFrameSU1 old = EventFrameBuilderSU1.capture(store, TARGET);
                checks.equal(129, old.size(), "Short eligible frame is recorded separately from training");
                checks.equal(1, store.saveDraws(List.of(fixture.get(350))), "Only the delivered draw closes the gap");
                PITSnapshotContractR952 replay = PITSnapshotContractR952.fromFrozenHistory(
                        TARGET, pinned.history, pinned.snapshotHash);
                checks.equal(pinned.snapshotHash, replay.snapshotHash, "Original frozen bytes still reproduce the hash");
                checks.equal(129, replay.history.size(), "Frozen input does not gain retroactive data");
                checks.require(!EventFrameBuilderSU1.unchanged(store, old),
                        "Old live frame cannot assert the newer source generation");
                PITSnapshotContractR952 current = PITSnapshotContractR952.capture(store, TARGET, 5000).newestContiguous();
                EventFrameSU1 now = EventFrameBuilderSU1.capture(store, TARGET);
                List<Models.Draw> verified = DevelopmentProductionCP12.selectHistoryCP12(
                        store.getDrawsBeforeIssue(TARGET, 5000), MINIMUM, false);
                compareSuffix(checks, current, now, verified);
                checks.equal(ROWS, now.size(), "Current frame covers all verified fixture draws");
                checks.require(now.sourceGeneration > old.sourceGeneration, "Backfill advanced the real SQL source clock");
                checks.require(!pinned.snapshotHash.equals(current.snapshotHash), "Current PIT has its own identity");
                scope(checks, store, now.cutoff);
                checks.record("frozenHistory", Map.of("oldPitSha256", pinned.snapshotHash,
                        "currentPitSha256", current.snapshotHash, "oldFrameSha256", old.contentHash,
                        "currentFrameSha256", now.contentHash, "oldGeneration", old.sourceGeneration,
                        "currentGeneration", now.sourceGeneration, "previousWait", held));
            } finally {
                store.close();
            }
        });
    }
}
