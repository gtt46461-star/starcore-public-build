package com.ethk3.novaonepro;
import java.io.*;
public final class NativeRetentionPageR76 {
 public static void main(String[] a)throws Exception {
  ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
  long[] rows=new long[a.length-5];for(int i=0;i<rows.length;i++)rows[i]=Long.parseLong(a[i+5]);
  final int[] calls={0};final int cancel=Integer.parseInt(a[4]);
  try {ReportNativeCP4.retentionPage(new File(a[0]),new File(a[1]),a[2],rows,32L<<20,Long.parseLong(a[3]),()->++calls[0]!=cancel);System.out.println("PASS calls="+calls[0]);}
  catch(ReportNativeCP4.Failure e){System.out.println("NATIVE_ERROR code="+e.nativeCode+" calls="+calls[0]);System.exit(3);}
 }
}
