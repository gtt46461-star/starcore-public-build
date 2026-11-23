package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.DatabaseCoordinator;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResearchRegistrationR34Test {
    private static boolean retry(String category, DatabaseCoordinator.CommitOutcome outcome) {
        return ResearchPoolR27.retryRolledBackLock(new DatabaseCoordinator.WriteRejectedR31(
                category,"R31_REGISTER_RESEARCH_TASKS","WAIT",outcome,null));
    }
    @Test public void retryCannotDuplicateACommitOrMaskFaults() {
        assertTrue(retry("LOCK_BUSY",DatabaseCoordinator.CommitOutcome.ROLLED_BACK));
        assertFalse(retry("LOCK_BUSY",DatabaseCoordinator.CommitOutcome.UNKNOWN));
        assertFalse(retry("LOCK_BUSY",DatabaseCoordinator.CommitOutcome.COMMITTED));
        assertFalse(retry("FAULT",DatabaseCoordinator.CommitOutcome.ROLLED_BACK));
    }
}
