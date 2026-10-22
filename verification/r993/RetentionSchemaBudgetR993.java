package com.ethk3.novaonepro;
import java.io.*;import java.lang.reflect.*;
/** Uses the retention owner's configured budget with the real vendored SQLite and complete product DDL. */
public final class RetentionSchemaBudgetR993 {
 public static void main(String[] args)throws Exception {
  ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
  Field field=RetentionArchiveIoCP12.class.getDeclaredField("NATIVE_BUDGET");field.setAccessible(true);
  long budget=field.getLong(null);
  try(ReportNativeCP4.RowReader reader=ReportNativeCP4.openRows(new File(args[0]),budget,()->true)){
   reader.nextRow("diagnostic_log","rowid",true,0);
   System.out.println("PASS RETENTION_FULL_SCHEMA budgetBytes="+budget+" peakBytes="+reader.nativePeakBytes()+" scope=Actual retention budget/JNI/product DDL; synthetic DB, not handset");
  }
 }
}
