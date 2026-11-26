package com.ethk3.novaonepro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReportResumeProgressTest {
    @Test public void largeDigestFinishesUnderContinuousRealtimePressureWithActualReadProgress() throws Exception {
        File snapshot=File.createTempFile("starcore-report-resume", ".db");
        try {
            byte[] block=new byte[32768];
            MessageDigest expected=MessageDigest.getInstance("SHA-256");
            try(FileOutputStream output=new FileOutputStream(snapshot)) {
                for(int i=0;i<1536;i++) {
                    block[0]=(byte)i;
                    output.write(block);
                    expected.update(block);
                }
            }
            ReportExportServiceCP4.StageTiming clock=new ReportExportServiceCP4.StageTiming();
            clock.enter(0,120000,true);
            long[] counts=new long[3]; // elapsed, pressure delay, hashed bytes
            String actual=ReportArchiveCP4.hash(snapshot,new ReportNativeCP4.Control() {
                @Override public boolean shouldContinue() {
                    long now=counts[0];
                    long until=clock.claimPressureYield(now,now+2000);
                    assertTrue(until-now<=100);
                    counts[1]+=until-now;
                    counts[0]=until+1250; // Measured slow reads, advanced on a deterministic monotonic clock.
                    return !clock.timedOut(counts[0]);
                }
                @Override public void onHashedBytes(long bytes) {
                    counts[2]+=bytes;
                    clock.progress(counts[0]);
                }
            });
            assertEquals(48L*1024*1024,counts[2]);
            assertTrue(counts[0]>ReportBudgetSU2.FULL_REPORT_STAGE_MS);
            assertTrue(counts[1]>0&&counts[1]<=counts[0]/10);
            assertEquals(ReportArchiveCP4.hex(expected.digest()),actual);
        } finally {
            assertTrue(snapshot.delete());
        }
    }

    @Test public void onlyRealProgressExtendsFullStageAndNeverReopensAnExpiredStage() {
        ReportExportServiceCP4.StageTiming clock=new ReportExportServiceCP4.StageTiming();
        clock.enter(0,120000,true);
        long idle=ReportBudgetSU2.FULL_REPORT_STAGE_MS;
        assertFalse(clock.timedOut(idle-1));
        clock.progress(idle-1);
        assertFalse(clock.timedOut(idle));
        assertTrue(clock.timedOut(2*idle-1));
        clock.progress(2*idle);
        assertTrue(clock.timedOut(2*idle));
        clock.enter(0,120000,false);
        clock.progress(119999);
        assertTrue(clock.timedOut(120000));
    }

    @Test public void densePressureDoesNotParkEveryChunkOrRenewWorkAtPhaseChanges() {
        ReportExportServiceCP4.StageTiming clock=new ReportExportServiceCP4.StageTiming();
        clock.enter(0,120000,true);
        assertEquals(100,clock.claimPressureYield(0,2000));
        for(long next=100;next<1000;next++)assertEquals(next,clock.claimPressureYield(next,next+2000));
        clock.enter(600,120000,true);
        assertEquals(999,clock.claimPressureYield(999,2999));
        assertEquals(1030,clock.claimPressureYield(1000,1030));
        assertEquals(2000,clock.claimPressureYield(2000,1999));
    }

    @Test public void cancellationStopsHashingWithoutCreditingTheUnreadChunk() throws Exception {
        File snapshot=File.createTempFile("starcore-report-cancel", ".db");
        try {
            try(FileOutputStream output=new FileOutputStream(snapshot)) { output.write(new byte[65536]); }
            long[] hashed={0};
            IOException failure=assertThrows(IOException.class,()->ReportArchiveCP4.hash(snapshot,new ReportNativeCP4.Control() {
                @Override public boolean shouldContinue() { return hashed[0]==0; }
                @Override public void onHashedBytes(long bytes) { hashed[0]+=bytes; }
            }));
            assertEquals("REPORT_CANCELLED",failure.getMessage());
            assertEquals(32768,hashed[0]);
        } finally {
            assertTrue(snapshot.delete());
        }
    }

    @Test public void pausedUiShowsRecordedBytesWithoutAnEverIncreasingClock() {
        String paused=CompleteReportFileR927.describeStageProgress("PAUSED",2000,"RESUME_VERIFY",1000,1048576,1500,2000);
        assertTrue(paused.contains("等待重试"));
        assertTrue(paused.contains("1.0 MiB"));
        assertEquals(paused,CompleteReportFileR927.describeStageProgress("PAUSED",2000,"RESUME_VERIFY",1000,1048576,1500,8000000));
        String observed=CompleteReportFileR927.describeStageProgress("PAUSED",2000,"RESUME_VERIFY",2500,2097152,3000,3850);
        assertTrue(observed.contains("已启动重试"));
        assertTrue(observed.contains("2.0 MiB"));
        assertTrue(CompleteReportFileR927.describeStageProgress("PAUSED",2000,"RESUME_VERIFY",2500,2097152,3000,8000).contains("等待重试"));
    }
}
