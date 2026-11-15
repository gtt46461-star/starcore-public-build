package com.ethk3.novaonepro;
import java.io.*;import java.security.*;import org.json.JSONObject;
/** The same streaming algorithm is used by the Android SAF owner and the host filesystem contract. */
public final class ReportSafCopyCP12 {
 private ReportSafCopyCP12(){}
 public interface Destination{OutputStream openOutputTruncating()throws IOException;InputStream openInput()throws IOException;}
 public static final class Receipt{public final long bytes;public final String sha256;private Receipt(long bytes,String sha){this.bytes=bytes;sha256=sha;}}
 public static Receipt copy(File source,String expectedSha,long expectedBytes,Destination destination,ReportNativeCP4.Control control)throws Exception{
  if(expectedSha==null||!expectedSha.matches("[0-9a-f]{64}")||expectedBytes<1||source.length()!=expectedBytes||!expectedSha.equals(ReportArchiveCP4.hash(source,control)))throw new IOException("REPORT_SAF_SOURCE_INTEGRITY");
  try(InputStream in=new FileInputStream(source);OutputStream out=destination.openOutputTruncating()){if(out==null)throw new IOException("REPORT_SAF_OUTPUT_UNAVAILABLE");byte[] buffer=new byte[32768];long bytes=0;int n;while((n=in.read(buffer))!=-1){check(control);out.write(buffer,0,n);bytes=Math.addExact(bytes,n);}if(bytes!=expectedBytes)throw new IOException("REPORT_SAF_SHORT_COPY");out.flush();}
  return verifyReadback(expectedSha,expectedBytes,destination,control);
 }
 public static Receipt verifyReadback(String expectedSha,long expectedBytes,Destination destination,ReportNativeCP4.Control control)throws Exception {
  if(expectedSha==null||!expectedSha.matches("[0-9a-f]{64}")||expectedBytes<1)throw new IOException("REPORT_SAF_EXPECTED_IDENTITY");
  MessageDigest hash=MessageDigest.getInstance("SHA-256");long bytes=0;try(InputStream in=destination.openInput()){if(in==null)throw new IOException("REPORT_SAF_READBACK_UNAVAILABLE");byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){check(control);bytes=Math.addExact(bytes,n);if(bytes>expectedBytes)throw new IOException("REPORT_SAF_VERIFY_FAILED");hash.update(buffer,0,n);}}
  String sha=ReportArchiveCP4.hex(hash.digest());if(bytes!=expectedBytes||!sha.equals(expectedSha))throw new IOException("REPORT_SAF_VERIFY_FAILED");
  // Closing a provider stream can deliver cancellation or interruption after its final read.
  // A success receipt is committed only after the closed stream and the control both agree.
  check(control);return new Receipt(bytes,sha);
 }
 /** Resume receipts are witnesses, not a second source of the immutable delivery identity. */
 public static Receipt verifySavedReceipt(JSONObject saved,String expectedName,String expectedSha,long expectedBytes,Destination destination,ReportNativeCP4.Control control)throws Exception {
  try{if(saved==null||!expectedName.equals(saved.getString("name"))||!expectedSha.equals(saved.getString("sha256"))||expectedBytes!=saved.getLong("bytes"))throw new IOException("REPORT_SAF_RECEIPT_IDENTITY");}
  catch(org.json.JSONException malformed){throw new IOException("REPORT_SAF_RECEIPT_IDENTITY",malformed);}
  return verifyReadback(expectedSha,expectedBytes,destination,control);
 }
 private static void check(ReportNativeCP4.Control control)throws IOException{if(Thread.currentThread().isInterrupted()||!control.shouldContinue())throw new InterruptedIOException("REPORT_SAF_CANCELLED");}
}
