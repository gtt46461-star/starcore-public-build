package com.ethk3.novaonepro;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportShareLeaseR34Test {
    @Test public void publicationRetriesOnlyRecoverableContention() {
        assertTrue(ReportPublicationCP12.retryDelivery(new ReportControlBudgetR31.Busy()));
        assertTrue(ReportPublicationCP12.retryDelivery(new ReportPublicationCP12.ShareBusyR31()));
        assertTrue(ReportPublicationCP12.retryDelivery(new StorageBudgetR31.Rejected("CENSUS_STALE")));
        assertFalse(ReportPublicationCP12.retryDelivery(new StorageBudgetR31.Rejected("FREE_SPACE_RESERVE")));
        assertFalse(ReportPublicationCP12.retryDelivery(new IOException("REPORT_DELIVERY_HASH_MISMATCH")));
    }

    @Test public void migrationWaitsForEveryRealFreezeAndUsesOnlyItsOwnServiceTurn() {
        DatabaseCoordinator.WriteContext schema=DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R34_RETENTION_UNIT");
        DatabaseCoordinator.WriteContext report=DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.REPORT,"R30_DECISION_EXPLAIN");
        assertTrue(DatabaseCoordinator.migrationServiceDue(schema,2000,1000,0));
        assertFalse(DatabaseCoordinator.migrationServiceDue(schema,1999,1000,0));
        assertFalse(DatabaseCoordinator.migrationServiceDue(schema,2000,1000,1));
        assertFalse(DatabaseCoordinator.migrationServiceDue(report,2000,1000,0));
    }

    @Test public void shareDeadlineUsesReservedMetadataAndCountsOtherJobLeases() throws Exception {
        File data = Files.createTempDirectory("starcore-share-deadline").toFile();
        File reports = new File(data, "files/r981_reports");
        File job = new File(reports, "jobs/" + "a".repeat(64));
        File prior = new File(reports, "jobs/" + "b".repeat(64));
        assertTrue(job.mkdirs());
        assertTrue(prior.mkdirs());
        File deadline = new File(job, "share-until.json");
        assertTrue("sharing a completed archive must use the report control budget",
                ReportControlBudgetR31.accepts(reports, deadline));
        assertFalse(ReportControlBudgetR31.accepts(reports, new File(job, "delivery/STARCORE_FULL_REPORT.zip")));
        StorageBudgetR31 budget = new StorageBudgetR31(data,
                new StorageBudgetR31.Config(1000000, 1000000, 1024, 8192, "SHARE_FIXTURE"), f -> 1L << 25);
        try (Closeable permit = ReportControlBudgetR31.admit(reports, deadline, 32, budget)) {
            assertNotNull(permit);
        }
        Files.write(new File(prior, "share-until.json.bak").toPath(), new byte[8192]);
        try (Closeable unexpected = ReportControlBudgetR31.admit(reports, deadline, 32, budget)) {
            fail("old and new AtomicFile versions must both be charged before sharing");
        } catch (IOException expected) {
            assertEquals("REPORT_CONTROL_QUOTA", expected.getMessage());
        }
    }
}
