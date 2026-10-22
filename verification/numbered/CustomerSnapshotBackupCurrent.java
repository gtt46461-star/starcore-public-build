package com.ethk3.novaonepro;
import java.io.File;

/** Runs only against a protected report snapshot copy; never emits customer rows. */
public final class CustomerSnapshotBackupCurrent {
    public static void main(String[] args) throws Exception {
        ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics", "host");
        File source=new File(args[0]), output=new File(args[1]);
        if (output.exists()) throw new AssertionError("fresh protected backup required");
        long start=System.nanoTime();
        ReportNativeCP4.backup(source,output,4L<<20,()->true);
        long peak;
        try (ReportNativeCP4.RowReader reader=ReportNativeCP4.openRows(output,4L<<20,()->true)) {
            peak=reader.nativePeakBytes();
        }
        if (!output.isFile() || output.length()==0) throw new AssertionError("backup absent");
        System.out.println("PASS|bytes="+output.length()+"|nativePeakBytes="+peak+"|elapsedMs="+(System.nanoTime()-start)/1000000+"|host native backup only; device/SAF unverified");
    }
}
