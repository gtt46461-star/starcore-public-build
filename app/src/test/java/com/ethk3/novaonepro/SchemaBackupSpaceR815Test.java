package com.ethk3.novaonepro;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Random;
import static org.junit.Assert.*;

public class SchemaBackupSpaceR815Test {
    private static final long MARGIN = 128L * 1024L * 1024L;

    @Test public void requiresFullCopyAndMarginWithoutOverflow() {
        long db = 9_656_647_680L;
        SchemaUpgradeCoordinatorR815.requireBackupSpace(db, db + MARGIN);
        try {
            SchemaUpgradeCoordinatorR815.requireBackupSpace(db, db + MARGIN - 1);
            fail("accepted an insufficient backup volume");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("R926_BACKUP_INSUFFICIENT_SPACE"));
        }
        try {
            SchemaUpgradeCoordinatorR815.requireBackupSpace(Long.MAX_VALUE, Long.MAX_VALUE);
            fail("overflow admitted an impossible backup");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("R926_BACKUP_INSUFFICIENT_SPACE"));
        }
    }

    @Test public void copyHashesTheSameBytesAndDoesNotReplaceBackup() throws Exception {
        Path dir = Files.createTempDirectory("r926-copy-test");
        try {
            Path source = dir.resolve("source"), backup = dir.resolve("backup");
            byte[] bytes = new byte[1_000_123];
            new Random(79).nextBytes(bytes);
            Files.write(source, bytes);
            String actual = SchemaUpgradeCoordinatorR815.copyAndSyncWithHash(source.toFile(), backup.toFile());
            StringBuilder expected = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes))
                expected.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            assertEquals(expected.toString(), actual);
            assertArrayEquals(bytes, Files.readAllBytes(backup));
            try {
                SchemaUpgradeCoordinatorR815.copyAndSyncWithHash(source.toFile(), backup.toFile());
                fail("existing backup was replaced");
            } catch (java.io.IOException expectedFailure) {
                assertEquals("BACKUP_DESTINATION_EXISTS", expectedFailure.getMessage());
            }
            assertArrayEquals(bytes, Files.readAllBytes(backup));
        } finally {
            try (java.util.stream.Stream<Path> files = Files.list(dir)) {
                files.forEach(p -> { try { Files.deleteIfExists(p); } catch (java.io.IOException e) { throw new RuntimeException(e); } });
            }
            Files.deleteIfExists(dir);
        }
    }

    @Test public void interruptionNeverPublishesAnIncompleteBackup() throws Exception {
        Path dir = Files.createTempDirectory("r926-cancel-test");
        try {
            Path source = dir.resolve("source"), backup = dir.resolve("backup");
            byte[] bytes = new byte[65536];
            new Random(8).nextBytes(bytes);
            Files.write(source, bytes);
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            Thread worker = new Thread(() -> {
                Thread.currentThread().interrupt();
                try { SchemaUpgradeCoordinatorR815.copyAndSyncWithHash(source.toFile(), backup.toFile()); }
                catch (Throwable t) { failure.set(t); }
            });
            worker.start();
            worker.join();
            assertTrue(failure.get() instanceof java.io.InterruptedIOException);
            assertFalse(Files.exists(backup));
            assertArrayEquals(bytes, Files.readAllBytes(source));
        } finally {
            try (java.util.stream.Stream<Path> files = Files.list(dir)) {
                files.forEach(p -> { try { Files.deleteIfExists(p); } catch (java.io.IOException e) { throw new RuntimeException(e); } });
            }
            Files.deleteIfExists(dir);
        }
    }
}
