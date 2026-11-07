package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import org.junit.Test;

/** R31-B05-C02#A2a: a simulated 24 h growth load (288 five-minute rounds of new durable artifacts) is bounded in
 * count and bytes by write-before-admission, and protected data is never deleted. Real files; fixture capacity. */
public final class R31StorageGrowthBoundTest {
    private static long bytesUnder(File dir) {
        long total = 0; File[] children = dir.listFiles(); if (children == null) return 0;
        for (File child : children) total += child.isDirectory() ? bytesUnder(child) : child.length();
        return total;
    }
    @Test public void twentyFourHourGrowthIsBoundedAndProtectedDataSurvives() throws Exception {
        File root = Files.createTempDirectory("r31-storage-growth-").toFile();
        File protectedDb = new File(root, "databases/main.db"); protectedDb.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(protectedDb)) { out.write(new byte[32_768]); }
        long hard = 256L << 10;
        StorageBudgetR31 budget = new StorageBudgetR31(root, new StorageBudgetR31.Config(hard, hard, 1024, 8192, "HOST_CAPACITY_FIXTURE"), f -> 100L << 20);
        int admitted = 0, denied = 0;
        for (int round = 0; round < 288; round++) {
            File artifact = new File(root, "files/r31-growth/round-" + round + "/artifact.bin");
            try (StorageBudgetR31.Reservation reservation = budget.reserve("growth-owner-" + round, round, StorageBudgetR31.Category.REPORT_PUBLISHED, "files/r31-growth/round-" + round, 8192, false)) {
                artifact.getParentFile().mkdirs();
                try (OutputStream out = reservation.wrap(artifact, new FileOutputStream(artifact))) { out.write(new byte[8192]); }
                admitted++;
            } catch (IOException refused) {
                assertTrue("refusal is an explicit storage admission reason: " + refused, String.valueOf(refused).contains("STORAGE_BUDGET_"));
                denied++;
            }
        }
        long grown = bytesUnder(new File(root, "files/r31-growth"));
        assertEquals(288, admitted + denied);
        assertTrue("growth stops at the budget instead of continuing for 24 h: admitted=" + admitted, denied > 0);
        assertTrue("new artifact count bounded by the budget: " + admitted, admitted <= hard / 8192);
        assertTrue("new artifact bytes bounded by the budget: " + grown, grown <= hard);
        assertTrue("total physical bytes bounded: " + bytesUnder(root), bytesUnder(root) <= hard + 65_536);
        assertEquals("protected data is never deleted or truncated", 32_768, protectedDb.length());
    }
}
