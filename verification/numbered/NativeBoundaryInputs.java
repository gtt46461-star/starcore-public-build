package com.ethk3.novaonepro;
import java.io.*;

/** Invalid callers must receive a typed failure before crossing into JNI. */
public final class NativeBoundaryInputs {
    public static void main(String[] args) throws Exception {
        ReportNativeCP4.loadForDiagnosticProcess("test:diagnostics", "test");
        File source = new File(args[1], "live.db"), dest = new File(args[1], "rejected-copy.db");
        try {
            switch (args[0]) {
                case "backup-null-control": ReportNativeCP4.backup(source, dest, 4L<<20, null); break;
                case "compress-null-control": ReportNativeCP4.compress(source, new ByteArrayOutputStream(), 4L<<20, null); break;
                case "compress-null-output": ReportNativeCP4.compress(source, null, 4L<<20, () -> true); break;
                case "backup-null-source": ReportNativeCP4.backup(null, dest, 4L<<20, () -> true); break;
                case "backup-null-dest": ReportNativeCP4.backup(source, null, 4L<<20, () -> true); break;
                case "compress-null-source": ReportNativeCP4.compress(null, new ByteArrayOutputStream(), 4L<<20, () -> true); break;
                default: throw new AssertionError("unknown test case");
            }
            throw new AssertionError("invalid input accepted");
        } catch (IOException rejected) {
            if (!"REPORT_NATIVE_STATE".equals(rejected.getMessage()))
                throw new AssertionError("invalid boundary lacks typed state reason", rejected);
            if (dest.exists()) throw new AssertionError("rejected request created destination");
            System.out.println("PASS|"+args[0]+"|typed IOException before native IO");
        }
    }
}
