package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Host transport; executes the product copy/readback owner. No Android provider claim. */
public final class ReportSafFinalizationR986 {
    private static int assertions;
    private interface Work { void run() throws Exception; }
    private static void reject(String reason, Work work) throws Exception {
        assertions++;
        try { work.run(); throw new AssertionError("accepted " + reason); }
        catch (IOException expected) {
            if (!reason.equals(expected.getMessage())) throw new AssertionError(expected);
        }
    }
    public static void main(String[] args) throws Exception {
        File root = new File(args[0]); root.mkdirs();
        File source = new File(root, "source.bin"), target = new File(root, "target.bin");
        byte[] bytes = new byte[65537];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte)(i * 37);
        Files.write(source.toPath(), bytes);
        String sha = ReportArchiveCP4.hash(source, () -> true);
        AtomicBoolean proceed = new AtomicBoolean(true);
        ReportSafCopyCP12.Destination normal = new ReportSafCopyCP12.Destination() {
            public OutputStream openOutputTruncating() throws IOException { return new FileOutputStream(target, false); }
            public InputStream openInput() throws IOException { return new FileInputStream(target); }
        };
        ReportSafCopyCP12.Receipt first = ReportSafCopyCP12.copy(source, sha, bytes.length, normal, proceed::get);
        assertions++;
        if (first.bytes != bytes.length || !sha.equals(first.sha256) || !java.util.Arrays.equals(bytes, Files.readAllBytes(target.toPath()))) throw new AssertionError("exact copy");
        ReportSafCopyCP12.Destination cancelledOnClose = new ReportSafCopyCP12.Destination() {
            public OutputStream openOutputTruncating() throws IOException { return new FileOutputStream(target, false); }
            public InputStream openInput() throws IOException {
                return new FilterInputStream(new FileInputStream(target)) {
                    @Override public void close() throws IOException { super.close(); proceed.set(false); }
                };
            }
        };
        // A cancellation accepted before the provider stream closes must prevent a success receipt.
        reject("REPORT_SAF_CANCELLED", () -> ReportSafCopyCP12.copy(source, sha, bytes.length, cancelledOnClose, proceed::get));
        proceed.set(true);
        ReportSafCopyCP12.Destination interruptedOnClose = new ReportSafCopyCP12.Destination() {
            public OutputStream openOutputTruncating() throws IOException { return new FileOutputStream(target, false); }
            public InputStream openInput() throws IOException {
                return new FilterInputStream(new FileInputStream(target)) {
                    @Override public void close() throws IOException { super.close(); Thread.currentThread().interrupt(); }
                };
            }
        };
        try { reject("REPORT_SAF_CANCELLED", () -> ReportSafCopyCP12.copy(source, sha, bytes.length, interruptedOnClose, proceed::get)); }
        finally { Thread.interrupted(); }
        ReportSafCopyCP12.Destination writeFailure = new ReportSafCopyCP12.Destination() {
            public OutputStream openOutputTruncating() { return new OutputStream() { public void write(int b) throws IOException { throw new IOException("ENOSPC_TEST_ONLY"); } }; }
            public InputStream openInput() { throw new AssertionError("readback after failed write"); }
        };
        reject("ENOSPC_TEST_ONLY", () -> ReportSafCopyCP12.copy(source, sha, bytes.length, writeFailure, () -> true));
        ReportSafCopyCP12.Destination noReadback = new ReportSafCopyCP12.Destination() {
            public OutputStream openOutputTruncating() throws IOException { return new FileOutputStream(target, false); }
            public InputStream openInput() { return null; }
        };
        reject("REPORT_SAF_READBACK_UNAVAILABLE", () -> ReportSafCopyCP12.copy(source, sha, bytes.length, noReadback, () -> true));
        Files.write(target.toPath(), new byte[]{1, 2, 3});
        reject("REPORT_SAF_VERIFY_FAILED", () -> ReportSafCopyCP12.verifyReadback(sha, bytes.length, normal, () -> true));
        Files.write(target.toPath(), java.util.Arrays.copyOf(bytes, bytes.length + 1));
        reject("REPORT_SAF_VERIFY_FAILED", () -> ReportSafCopyCP12.verifyReadback(sha, bytes.length, normal, () -> true));
        ReportSafCopyCP12.copy(source, sha, bytes.length, normal, () -> true);
        assertions++;
        if (!sha.equals(ReportArchiveCP4.hash(target, () -> true))) throw new AssertionError("recovery exact bytes");
        System.out.println("PASS R986_SAF_FINALIZATION assertions=" + assertions + " transport=HOST_FILESYSTEM DEVICE_NOT_VERIFIED");
    }
}
