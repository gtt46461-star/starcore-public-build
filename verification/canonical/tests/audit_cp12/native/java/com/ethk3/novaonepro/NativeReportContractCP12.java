package com.ethk3.novaonepro;
import java.io.*;import java.nio.file.*;import java.util.*;import java.security.*;
public final class NativeReportContractCP12 {
 static int assertions;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 interface Work {void run()throws Exception;}
 static void reject(Work w,String m)throws Exception{assertions++;try{w.run();throw new AssertionError(m);}catch(IOException|IllegalArgumentException expected){}}
 static final ReportNativeCP4.Control YES=()->true;
 static final class DigestSink extends OutputStream { final MessageDigest sha; long bytes; DigestSink()throws Exception{sha=MessageDigest.getInstance("SHA-256");}public void write(int b){sha.update((byte)b);bytes++;}public void write(byte[] b,int o,int n){sha.update(b,o,n);bytes+=n;}}
 static byte[] digest(File f)throws Exception{MessageDigest m=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];for(int n;(n=in.read(b))>=0;)if(n>0)m.update(b,0,n);}return m.digest();}
 public static void main(String[] args)throws Exception{
  File dir=new File(args[0]);dir.mkdirs();File source=new File(dir,"payload.bin"),compressed=new File(dir,"payload.zst");
  reject(()->ReportNativeCP4.compress(source,new ByteArrayOutputStream(),2L<<20,YES),"mustloadfirst");
  check(!source.exists(),"host input fresh");
  try{ReportNativeCP4.loadForDiagnosticProcess("wrong","test");throw new AssertionError("wrongprocess");}catch(IllegalStateException good){assertions++;}
  ReportNativeCP4.loadForDiagnosticProcess("test:diagnostics","test");
  byte[] block=new byte[8192];new Random(7801).nextBytes(block);long size=(96L<<20)+17;
  try(OutputStream out=new FileOutputStream(source)){for(long left=size;left>0;){int n=(int)Math.min(left,block.length);out.write(block,0,n);left-=n;}}
  try(OutputStream out=new FileOutputStream(compressed)){ReportNativeCP4.compress(source,out,2L<<20,YES);}
  DigestSink sink=new DigestSink();try(InputStream in=new FileInputStream(compressed)){ReportNativeCP4.decompress(in,sink,size,4L<<20,YES);}
  check(sink.bytes==size,"complete96MiB");check(Arrays.equals(digest(source),sink.sha.digest()),"lossless96MiB");
  byte[] packed=Files.readAllBytes(compressed.toPath());check(packed.length<1<<20,"boundedtestfixture");
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(Arrays.copyOf(packed,packed.length-1)),new DigestSink(),size,4L<<20,YES),"truncated");
  byte[] trailing=Arrays.copyOf(packed,packed.length+1);trailing[trailing.length-1]=1;
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(trailing),new DigestSink(),size,4L<<20,YES),"trailing");
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(packed),new DigestSink(),size-1,4L<<20,YES),"oversizedoutput");
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(packed),new DigestSink(),size+1,4L<<20,YES),"shortoutput");
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(packed),new DigestSink(),size,4L<<20,()->false),"cancel");
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(packed),new DigestSink(),size,1024,YES),"memoryguard");
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(packed),new OutputStream(){public void write(int b)throws IOException{throw new IOException("SINK_FAIL");}public void write(byte[] b,int o,int n)throws IOException{throw new IOException("SINK_FAIL");}},size,4L<<20,YES),"writeexception");
  byte[] joined=new byte[packed.length*2];System.arraycopy(packed,0,joined,0,packed.length);System.arraycopy(packed,0,joined,packed.length,packed.length);
  reject(()->ReportNativeCP4.decompress(new ByteArrayInputStream(joined),new DigestSink(),size*2,4L<<20,YES),"multiframe");
  File archive=new File(dir,"archive");archive.mkdirs();Files.write(new File(archive,"manifest.jsonl").toPath(),new byte[]{'x'});Files.write(new File(archive,"P000001.bin").toPath(),new byte[]{1});
  ReportNativeCP4.verifyArchiveDirectory(archive,1,YES);assertions++;
  Files.createSymbolicLink(new File(archive,"P000002.bin").toPath(),new File(archive,"P000001.bin").toPath());
  reject(()->ReportNativeCP4.verifyArchiveDirectory(archive,2,YES),"partsymlink");Files.delete(new File(archive,"P000002.bin").toPath());
  Files.move(new File(archive,"manifest.jsonl").toPath(),new File(dir,"manifest-real.jsonl").toPath());Files.createSymbolicLink(new File(archive,"manifest.jsonl").toPath(),new File(dir,"manifest-real.jsonl").toPath());
  reject(()->ReportNativeCP4.verifyArchiveDirectory(archive,1,YES),"manifestsymlink");
  File live=new File(dir,"live.db"),copy=new File(dir,"snapshot.db");
  byte[] sourceHashBefore=digest(live);ReportNativeCP4.backup(live,copy,4L<<20,YES);assertions++;
  check(Arrays.equals(digest(live),sourceHashBefore),"backupsourceunchanged");
  reject(()->ReportNativeCP4.backup(live,live,4L<<20,YES),"backupself");
  reject(()->ReportNativeCP4.backup(live,copy,4L<<20,YES),"nooverwrite");
  File cancelled=new File(dir,"cancelled.db");reject(()->ReportNativeCP4.backup(live,cancelled,4L<<20,()->false),"backupcancel");check(!cancelled.exists(),"cancelremovespartial");
  try(ReportNativeCP4.RowReader reader=ReportNativeCP4.openRows(copy,4L<<20,YES)){
   reject(()->ReportNativeCP4.openRows(copy,4L<<20,YES),"singlereader");
   reject(()->ReportNativeCP4.compress(source,new DigestSink(),2L<<20,YES),"readercompressisolation");
   Long row=reader.nextRow("cp12_jni_fixture","rowid",true,0);check(row!=null&&row==Long.MIN_VALUE,"minrowid");
   ReportNativeCP4.RowReader.Cell text=reader.cell("cp12_jni_fixture","text_payload","rowid",row);check(text.type==3&&text.length==2097152,"textshape");
   byte[] chunk=new byte[8192];long seen=0;while(seen<text.length){int n=(int)Math.min(chunk.length,text.length-seen);reader.read(seen,chunk,n);for(int i=0;i<n;i++)check(chunk[i]=='A',"textbyte");seen+=n;}
   ReportNativeCP4.RowReader.Cell blob=reader.cell("cp12_jni_fixture","blob_payload","rowid",row);check(blob.type==4&&blob.length==2097152,"blobshape");
   seen=0;while(seen<blob.length){int n=(int)Math.min(chunk.length,blob.length-seen);reader.read(seen,chunk,n);for(int i=0;i<n;i++)check(chunk[i]==0,"blobbyte");seen+=n;}
   check(reader.nextRow("cp12_jni_fixture","rowid",false,row)==null,"rowsEOF");check(reader.nativePeakBytes()<=4L<<20,"nativebudget");
  }
  System.out.println("{\"status\":\"PASS\",\"assertions\":"+assertions+",\"raw_bytes\":"+size+",\"scope\":\"Actual host JNI compression/decompression/directory verification; not AndroidABI/Service/SAF/device\"}");
 }
}
