package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual journal/ACK publication with real temporary files; no phone result. */
public class R32FailureReceiptTest {
    static String id(int n){return String.format(java.util.Locale.ROOT,"%064x",n);}
    @Test public void originalOpenFailureSurvivesTheFailedBeforeRecoveryPublicationWindow()throws Exception{
        File app=Files.createTempDirectory("r32-failed-before-recovery-").toFile(),files=new File(app,"files");files.mkdirs();
        StorageBudgetR31 budget=new StorageBudgetR31(app,new StorageBudgetR31.Config(64L<<20,64L<<20,1024,2L<<20,"HOST_FAILURE_PUBLICATION"),f->512L<<20);
        try(Closeable fixture=ReportIoR31.hostFixture(app,budget);ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){
            ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id(32),id(32),id(33),"FULL_REPORT",1234);journal.create(job);journal.transition(job,ReportJobJournalCP12.Stage.BACKUP,1235);
            File missing=new File(app,"source-missing-r32.db");FileNotFoundException original;
            try(FileInputStream in=new FileInputStream(missing)){throw new AssertionError("missing file unexpectedly exists");}catch(FileNotFoundException e){original=e;}
            job.lastReason="FileNotFoundException";ReportJobJournalCP12.rememberFailureR32(job,"BACKUP",original);
            journal.transition(job,ReportJobJournalCP12.Stage.FAILED,1236);
            File ack=new File(journal.root(),"requests/"+job.requestId+".ack.json");byte[] before=Files.readAllBytes(ack.toPath());assertTrue(before.length<8192);
            assertFalse(new File(journal.root(),"recovery/"+job.requestId+"/receipt.json").exists());
            JSONObject state=ReportJobJournalCP12.requestStateR31(journal.root(),job.requestId);
            String detail=ReportJobJournalCP12.failureDetailR31(journal.root(),state).text;
            assertTrue(detail,detail.contains("source-missing-r32.db"));assertTrue(detail.contains("FileNotFoundException"));assertTrue(detail.contains("任务回执内保存"));assertFalse(detail.contains("原始异常暂不可用"));
            ReportJobJournalCP12.Job disk=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(journal.directory(job),"job.json"),65536));assertEquals(job.failureDetailR32,disk.failureDetailR32);
            assertArrayEquals(before,Files.readAllBytes(ack.toPath()));
            job.recoveryPublicationR32="FAILED";job.recoveryFailureR32="RECOVERY_METADATA_UNWRITABLE_FIXTURE";journal.checkpoint(job,1237);journal.acknowledge(job.requestId,job);
            detail=ReportJobJournalCP12.failureDetailR31(journal.root(),ReportJobJournalCP12.requestStateR31(journal.root(),job.requestId)).text;
            assertTrue(detail.contains("source-missing-r32.db"));assertTrue(detail.contains("RECOVERY_METADATA_UNWRITABLE_FIXTURE"));assertTrue(detail.contains("恢复包状态=FAILED"));
        }
    }
    @Test public void boundedOriginalIsUtf8LimitedAndCredentialsAreMasked()throws Exception{
        StringBuilder message=new StringBuilder("missing-file.db API_KEY=unit-private-secret\n");for(int i=0;i<9000;i++)message.append("故障");
        String value=ReportJobJournalCP12.boundedFailureR32(new FileNotFoundException(message.toString()),3072);
        assertTrue(value.getBytes(StandardCharsets.UTF_8).length<=3072);assertFalse(value.contains("unit-private-secret"));assertTrue(value.contains("missing-file.db"));assertFalse(Character.isHighSurrogate(value.charAt(value.length()-1)));
    }
    @Test public void aLaterAttemptRecordsItsOwnPrimaryFailureAndOldJournalsStillRead()throws Exception{
        ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id(34),id(34),id(33),"FULL_REPORT",1);
        JSONObject legacy=job.json();assertFalse(legacy.has("failureDetailR32"));assertEquals("",ReportJobJournalCP12.Job.parse(legacy.toString()).failureDetailR32);
        ReportJobJournalCP12.rememberFailureR32(job,"BACKUP",new IOException("OLD_TRANSIENT_DEPENDENCY"));
        ReportJobJournalCP12.rememberFailureR32(job,"SNAPSHOT_HASH",new FileNotFoundException("CURRENT_PRIMARY_MISSING_FILE"));
        assertFalse(job.failureDetailR32.contains("OLD_TRANSIENT_DEPENDENCY"));assertTrue(job.failureDetailR32.contains("CURRENT_PRIMARY_MISSING_FILE"));assertEquals("SNAPSHOT_HASH",ReportJobJournalCP12.Job.parse(job.json().toString()).failurePhaseR32);
    }
}
