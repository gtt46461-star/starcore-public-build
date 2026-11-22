package com.ethk3.novaonepro;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ReportSourceJournalR335Test {
    private static final String ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String RELEASE = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test public void installedReceiptWithoutPreparationKeepsOriginalSnapshotIdentity() throws Exception {
        ReportJobJournalCP12.Job original = new ReportJobJournalCP12.Job(ID, ID, RELEASE, "FULL_REPORT", 124L);
        original.snapshotSha = RELEASE;
        JSONObject legacy = original.json();
        legacy.remove("reportSourcePreparation");
        ReportJobJournalCP12.Job restored = ReportJobJournalCP12.Job.parse(legacy.toString());
        assertEquals("", restored.reportSourcePreparation);
        assertEquals(RELEASE, restored.snapshotSha);
        assertEquals(ID, restored.jobId);
    }

    @Test public void preparationObservationDoesNotChangeReportVerdict() throws Exception {
        ReportJobJournalCP12.Job job = new ReportJobJournalCP12.Job(ID, ID, RELEASE, "FULL_REPORT", 124L);
        for (String status : new String[]{"REGISTERED_REPORT_SOURCES_AVAILABLE", "REGISTERED_SOURCE_OWNER_STALLED"}) {
            job.reportSourcePreparation = status;
            ReportJobJournalCP12.Job restored = ReportJobJournalCP12.Job.parse(job.json().toString());
            assertEquals(status, restored.reportSourcePreparation);
            assertFalse(restored.checksComplete);
            assertFalse(restored.evidenceComplete);
            assertFalse(restored.allChecksPassed);
        }
    }
}
