package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DatabaseCoordinator.CommitOutcome;
import com.ethk3.novaonepro.StartupStageJournalR341;
import org.junit.Test;

public final class R342StartupFailureDispositionTest {
    @Test public void typedStartupFailureRemainsTheSameAcrossWrappers() {
        for (String category : new String[]{"RESOURCE_MEMORY", "DEPENDENCY", "MISSING_SOURCE", "CANCELLED", "BUG"}) {
            DatabaseCoordinator.WriteRejectedR31 issue = new DatabaseCoordinator.WriteRejectedR31(
                    category, "STARTUP_PROBE", "BEFORE_SQL", CommitOutcome.ROLLED_BACK,
                    new IllegalStateException("INNER_CAUSE_NO_CATEGORY"));
            Throwable wrapped = new IllegalStateException("STARTUP_WRAPPER", new IllegalStateException("OUTER", issue));
            assertEquals(category, FailureDispositionR31.classify(wrapped).category);
            assertEquals(StartupStageJournalR341.classify(issue), StartupStageJournalR341.classify(wrapped));
            assertEquals("BUG".equals(category), FailureDispositionR31.classify(wrapped).consumesFaultBudget());
        }
        assertEquals(StartupStageJournalR341.Outcome.WAIT_DEPENDENCY,
                StartupStageJournalR341.classify(new IllegalStateException("WRAPPER",
                        new DatabaseCoordinator.SchemaPendingR31("STARTUP_SCHEMA", "INDEX_NOT_READY"))));
        assertEquals(StartupStageJournalR341.Outcome.WAIT_RESOURCE,
                StartupStageJournalR341.classify(new DatabaseCoordinator.WriteRejectedR31(
                        "RESOURCE_MEMORY", "STARTUP", "ADMISSION", CommitOutcome.ROLLED_BACK, null)));
    }

    @Test public void cancellationCannotForceAnUncertainCommitReplay() {
        DatabaseCoordinator.WriteRejectedR31 wait = new DatabaseCoordinator.WriteRejectedR31(
                "DEPENDENCY", "STARTUP", "WAIT", CommitOutcome.ROLLED_BACK, new InterruptedException());
        assertEquals(StartupStageJournalR341.Outcome.INTERRUPTED, StartupStageJournalR341.classify(wait));
        DatabaseCoordinator.WriteRejectedR31 uncertain = new DatabaseCoordinator.WriteRejectedR31(
                "DEPENDENCY", "STARTUP", "END_TX", CommitOutcome.UNKNOWN, new InterruptedException());
        assertTrue(FailureDispositionR31.classify(uncertain).requiresCommitResolution);
        assertEquals(StartupStageJournalR341.Outcome.WAIT_DEPENDENCY, StartupStageJournalR341.classify(uncertain));
        assertEquals(StartupStageJournalR341.Outcome.FATAL, StartupStageJournalR341.classify(
                new IllegalStateException("WRAPPER", new LinkageError("STARTUP_LINKAGE"))));
    }
}
