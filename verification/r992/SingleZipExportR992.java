package com.ethk3.novaonepro;
import java.io.*;import java.util.*;import java.util.zip.*;import java.security.*;import org.json.*;
/** Actual JNI/archive/publication/SAF code; full product schema and incompressible >8MiB fixture. */
public final class SingleZipExportR992 {
 static int assertions;static void need(boolean value,String code){assertions++;if(!value)throw new AssertionError(code);}
 public static void main(String[]args)throws Exception {
  File root=new File(args[1]);root.mkdirs();String id=String.format(Locale.ROOT,"%064x",992);
  ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
  File job=new File(root,"r981_reports/jobs/"+id);job.mkdirs();File snapshot=new File(job,"snapshot.db");
  ReportNativeCP4.backup(new File(args[0]),snapshot,32L<<20,()->true);need(snapshot.length()>6L<<20,"fullProductSchema");
  File random=new File(job,"random.bin");try(FileOutputStream out=new FileOutputStream(random)){Random rng=new Random(992);byte[] b=new byte[32768];for(int i=0,blocks=(args.length>2?Integer.parseInt(args[2]):12)*32;i<blocks;i++){rng.nextBytes(b);out.write(b);}out.getFD().sync();}
  File archive=new File(job,"archive-1");archive.mkdir();
  try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,id)){writer.add("random.bin",random,4L<<20,()->true);writer.add("snapshot.db",snapshot,4L<<20,()->true);writer.finish(true,true,"R992_SYNTHETIC_FULL_PRODUCT_DDL_NOT_USER_DB");}
  ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),()->true,null);
  ReportPublicationCP12.prepareSingleZip(job,archive,id,proof.manifestSha256,true,true,false,()->true);
  JSONObject index=new JSONObject(ReportJobJournalCP12.readAtomic(new File(job,"delivery/index.json"),65536));
  JSONArray rows=index.getJSONArray("files");need(rows.length()==1,"oneZip");JSONObject row=rows.getJSONObject(0);File zip=new File(job,row.getString("path"));need(zip.length()>8L<<20,"zipAboveOldLimit");need(!index.getBoolean("allChecksPassed"),"preserveFailedHealth");
  int entries=0;try(ZipInputStream in=new ZipInputStream(new FileInputStream(zip))){ZipEntry entry;byte[] b=new byte[32768];while((entry=in.getNextEntry())!=null){entries++;MessageDigest digest=MessageDigest.getInstance("SHA-256");int n;while((n=in.read(b))!=-1)digest.update(b,0,n);
    String actual=ReportArchiveCP4.hex(digest.digest());if(entry.getName().equals("snapshot.db"))need(actual.equals(ReportArchiveCP4.hash(snapshot,()->true)),"databaseAllBytes");if(entry.getName().equals("random.bin"))need(actual.equals(ReportArchiveCP4.hash(random,()->true)),"incompressibleAllBytes");in.closeEntry();}}
  need(entries==3,"allMembersAndManifest");
  File destination=new File(root,"saved-report.zip");ReportSafCopyCP12.Destination port=new ReportSafCopyCP12.Destination(){public OutputStream openOutputTruncating()throws IOException{return new FileOutputStream(destination);}public InputStream openInput()throws IOException{return new FileInputStream(destination);}};
  ReportSafCopyCP12.Receipt receipt=ReportSafCopyCP12.copy(zip,row.getString("sha256"),zip.length(),port,()->true);need(receipt.bytes==zip.length(),"largeSafReadback");need(receipt.sha256.equals(row.getString("sha256")),"largeSafHash");
  try{ReportSafCopyCP12.copy(zip,row.getString("sha256"),zip.length(),port,()->false);throw new AssertionError("cancel");}catch(IOException expected){need(expected.getMessage().contains("CANCELLED"),"cancelReason");}
  System.out.println("{\"status\":\"PASS\",\"assertions\":"+assertions+",\"zipBytes\":"+zip.length()+",\"snapshotBytes\":"+snapshot.length()+",\"scope\":\"Actual current JNI/publication/SAF copy; full product DDL, synthetic data; Android device and user database not verified\"}");
 }
}
