package com.ethk3.novaonepro;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public final class StorageBudgetLiveScopeR34Test {
    @Test public void distinguishVanishedEntryFromStillAllocatedFileAndDanglingLink() throws Exception {
        Path dir = Files.createTempDirectory("r34-scope");
        Path regular = dir.resolve("lease");
        Path link = dir.resolve("link");
        try {
            Files.write(regular, new byte[179]);
            File enumerated;
            try (DirectoryCursorR31 entries = DirectoryCursorR31.open(dir.toFile())) {
                enumerated = entries.next();
            }
            assertEquals(179, StorageBudgetR31.ScopeEntry.read(enumerated).length);
            assertEquals(179, StorageBudgetR31.scopeBytes(dir.toFile()));
            Files.delete(regular);
            assertNull(StorageBudgetR31.ScopeEntry.read(enumerated));
            assertTrue(StorageBudgetR31.ScopeEntry.confirmedAbsent(enumerated));
            assertEquals(0, StorageBudgetR31.scopeBytes(dir.toFile()));
            Files.createSymbolicLink(link, regular);
            assertFalse(StorageBudgetR31.ScopeEntry.confirmedAbsent(link.toFile()));
            try {
                StorageBudgetR31.scopeBytes(dir.toFile());
                fail("A dangling link must still be refused");
            } catch (StorageBudgetR31.Rejected refused) {
                assertTrue(refused.getMessage().contains("SCOPE_SYMLINK"));
            }
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(regular);
            Files.deleteIfExists(dir);
        }
    }
}
