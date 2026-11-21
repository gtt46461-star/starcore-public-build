package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import org.junit.Test;

public final class VivoReportBudgetTest {
    @Test public void frozenCopiesReserveTheirFullLengthAndOnlyUnmaterializedFiles() throws Exception {
        long missing = (3L << 30) + 71231;
        assertEquals(missing + (88L << 10), ReportPhaseBudgetR31.frozenCopyBytes(missing, 2));
        assertEquals(64L << 10, ReportPhaseBudgetR31.frozenCopyBytes(0, 0));
        assertThrows(java.io.IOException.class, () -> ReportPhaseBudgetR31.frozenCopyBytes(Long.MAX_VALUE, 1));
    }
    @Test public void unrelatedDeepArchiveDoesNotStopReportButOtherOwnersAndFreeSpaceStillCount() throws Exception {
        File root = Files.createTempDirectory("r335-scoped-report").toFile();
        File archive = new File(root, "files/protected");
        for (int i = 0; i < 16; i++) archive = new File(archive, "level");
        assertTrue(archive.mkdirs());
        File original = new File(archive, "original");
        Files.write(original.toPath(), new byte[]{3, 5, 8});
        long[] free = {16L << 20};
        StorageBudgetR31.Config policy = new StorageBudgetR31.Config(1024, 2048, 1024, 128, "TEST_SCOPED", true);
        StorageBudgetR31 disk = new StorageBudgetR31(root, policy, ignored -> free[0]);
        assertEquals("UNKNOWN_INCOMPLETE_CENSUS", disk.snapshot().get("state"));
        try (StorageBudgetR31.Reservation first = disk.reserve("report:first", 1,
                StorageBudgetR31.Category.REPORT_WORKSPACE, "files/current", 4L << 20, true)) {
            try (StorageBudgetR31.Reservation second = disk.reserve("report:second", 2,
                    StorageBudgetR31.Category.REPORT_WORKSPACE, "files/second", 2L << 20, true)) {
                free[0] = 7L << 20;
                try {
                    first.ensureAdditional(6L << 20, 6L << 20, true);
                    fail("Both promises must fit in the available bytes");
                } catch (StorageBudgetR31.Rejected failure) {
                    assertEquals("FREE_SPACE_RESERVE", failure.reason);
                }
            }
            first.ensureAdditional(6L << 20, 6L << 20, true);
        }
        assertArrayEquals(new byte[]{3, 5, 8}, Files.readAllBytes(original.toPath()));
    }

    @Test public void largeExistingDatabaseCanReserveBackupAndPublishWithProtectedHistory() throws Exception {
        final long database = 1373483008L, wal = 824032L;
        long source = database + wal;
        long backup = source + source / 16 + (16L << 20);
        File root = Files.createTempDirectory("r335-upgrade-report").toFile();
        File db = new File(root, "databases/live.db");
        assertTrue(db.getParentFile().mkdirs());
        try (RandomAccessFile original = new RandomAccessFile(db, "rw")) {
            original.setLength(database);
        }
        // The fixture supplies one retained GiB. The phone's quick report measures only DB and WAL.
        File pinned = new File(root, "files/pinned-original");
        assertTrue(pinned.getParentFile().mkdirs());
        try (RandomAccessFile original = new RandomAccessFile(pinned, "rw")) {
            original.setLength(1L << 30);
        }
        StorageBudgetR31.Config policy = StorageBudgetR31.Config.production();
        StorageBudgetR31.Config fixture = new StorageBudgetR31.Config(policy.normalHard, policy.transientHard,
                policy.freeFloor, policy.metadataHeadroom, "FIELD_FILE_LENGTH_FIXTURE",policy.capacityDriven);
        StorageBudgetR31 disk = new StorageBudgetR31(root, fixture, ignored -> 57975308288L);
        long initial = ReportPhaseBudgetR31.initial(backup);
        try (StorageBudgetR31.Reservation job = disk.reserve("report:fixture", 11,
                StorageBudgetR31.Category.REPORT_WORKSPACE, "files/report", initial, initial * 4, true)) {
            long future = ReportPhaseBudgetR31.archiveFuture(source, 128);
            job.ensureAdditional(future, ReportPhaseBudgetR31.MAX_WRITTEN, true);
        }
        assertEquals(database, db.length());
        assertEquals(1L << 30, pinned.length());
        assertTrue(policy.capacityDriven);
    }

    @Test public void reportGrowsPastRetentionTargetsAndInsufficientSpacePreservesOriginal() throws Exception {
        File root=Files.createTempDirectory("r335-stream").toFile();
        File files=new File(root,"files"); assertTrue(files.mkdir());
        File original=new File(files,"original"); Files.write(original.toPath(),new byte[4096]);
        final long[] free={32L<<20};
        StorageBudgetR31.Config policy=new StorageBudgetR31.Config(1024,2048,1024,128,"TEST_CAPACITY",true);
        StorageBudgetR31 disk=new StorageBudgetR31(root,policy,ignored->free[0]);
        File report=new File(files,"report"); assertTrue(report.mkdir());
        File output=new File(report,"full.zip");
        try(StorageBudgetR31.Reservation job=disk.reserve("report:growing",2,StorageBudgetR31.Category.REPORT_WORKSPACE,
                "files/report",128,128,true);
            java.io.OutputStream sink=job.wrap(output,new java.io.FileOutputStream(output))) {
            sink.write(new byte[16384]); sink.flush();
            assertEquals(16384,output.length());
            free[0]=1040;
            try { sink.write(new byte[512]); fail("physical free space must protect the device"); }
            catch(StorageBudgetR31.Rejected expected){assertEquals("FREE_SPACE_RESERVE",expected.reason);}
        }
        assertEquals(4096,original.length());
        assertEquals(16384,output.length());
    }
}
