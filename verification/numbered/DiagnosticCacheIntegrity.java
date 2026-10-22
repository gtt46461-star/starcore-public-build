package com.ethk3.novaonepro;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Exercises the production content-addressed detail cache, including same-length corruption. */
public final class DiagnosticCacheIntegrity {
    private static int checks;
    private static void require(boolean ok, String assertion) {
        checks++;
        if (!ok) throw new AssertionError(assertion);
    }
    public static void main(String[] args) throws Exception {
        File directory = new File(args[0]);
        require(directory.mkdirs() || directory.isDirectory(), "fresh cache directory");
        String body = "当前真实详情\n".repeat(2000);
        File ready = DiagnosticFileStoreR941.write(directory, body);
        byte[] expected = body.getBytes(StandardCharsets.UTF_8);
        require(java.util.Arrays.equals(Files.readAllBytes(ready.toPath()), expected), "original exact bytes");
        byte[] damaged = expected.clone();
        damaged[damaged.length / 2] ^= 1;
        Files.write(ready.toPath(), damaged);
        require(ready.length() == expected.length, "counterexample preserves length");
        try {
            DiagnosticFileStoreR941.write(directory, body);
            throw new AssertionError("same-length corrupt cache was accepted as exact original");
        } catch (IOException rejected) {
            require("REPORT_HASH_CONTENT_CONFLICT".equals(rejected.getMessage()), "typed content conflict");
        }
        require(java.util.Arrays.equals(Files.readAllBytes(ready.toPath()), damaged), "failure preserves conflicting artifact");
        for (File file : directory.listFiles())
            require(!file.getName().endsWith(".partial"), "failed write releases temporary file");
        require(ready.delete(), "explicit removal of corrupt cache permits retry");
        File recovered = DiagnosticFileStoreR941.write(directory, body);
        require(java.util.Arrays.equals(Files.readAllBytes(recovered.toPath()), expected), "retry restores exact bytes");
        require(DiagnosticFileStoreR941.write(directory, body).equals(recovered), "verified healthy cache remains idempotent");
        System.out.println("PASS|checks=" + checks + "|scope=current production detail cache; not full-report/SAF/device");
    }
}
