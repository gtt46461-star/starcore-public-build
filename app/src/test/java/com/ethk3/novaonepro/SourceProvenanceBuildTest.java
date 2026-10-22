package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual build inputs, production report consumer and stale frozen evidence guards. */
public final class SourceProvenanceBuildTest {
    private static String hex(byte[] value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value) out.append(String.format(Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }

    @Test public void generatedIdentityEqualsEveryCurrentJavaInput() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (Stream<Path> files = Files.walk(root.resolve("app/src/main/java"))) {
            Path[] inputs = files.filter(p -> p.toString().endsWith(".java")
                    && !p.getFileName().toString().equals("SourceManifestR815.java"))
                    .sorted(Comparator.comparing(p -> root.relativize(p).toString())).toArray(Path[]::new);
            assertTrue(inputs.length > 800);
            for (Path p : inputs) {
                String hash = hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
                digest.update(("PATH=" + root.relativize(p).toString().replace('\\', '/')
                        + "\nSHA256=" + hash + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        assertEquals(hex(digest.digest()), SourceManifestR815.SOURCE_TREE_SHA256);
        assertTrue(SourceManifestR815.valid());
    }

    @Test public void productionSnapshotConsumesThisBuildAndPreservesRuntimeBoundary() {
        BuildProvenanceR815.Snapshot snapshot = BuildProvenanceR815.fromGeneratedManifest();
        assertTrue(snapshot.valid);
        assertEquals(SourceManifestR815.SOURCE_TREE_SHA256, snapshot.sourceTreeSha256);
        assertEquals(SourceManifestR815.RESOURCE_MANIFEST_SHA256, snapshot.resourceManifestSha256);
        assertEquals(SourceManifestR815.TOOLCHAIN_SHA256, snapshot.toolchainSha256);
        assertEquals("GENERATED_MANIFEST_READY", snapshot.status);
        assertEquals("", snapshot.apkSha256);
        assertEquals("", snapshot.classesDexSha256);
        assertFalse(BuildProvenanceR815.verifyRuntime(null).valid);
    }

    @Test public void runtimeTrainingIdentityMatchesCurrentGeneratedBuild() {
        assertEquals(ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,
                TrainingCodeIdentityR964.SHA256);
        assertTrue(TrainingContinuationR973.compatible(ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256));
    }
}
