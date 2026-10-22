package com.ethk3.novaonepro;
import java.io.*;import java.util.*;import java.nio.file.*;
public final class ArchivePipelineActualCP12 {
 static int n;static void ok(boolean x,String m){n++;if(!x)throw new AssertionError(m);}
 interface Work{void run()throws Exception;}static void reject(Work w,String m)throws Exception{n++;try{w.run();throw new AssertionError(m);}catch(IOException|IllegalArgumentException expected){}}
 static final ReportNativeCP4.Control YES=()->true;
 public static void main(String[] a)throws Exception{
  File dir=new File(a[0]);if(!dir.mkdir())throw new IOException("FRESH_FIXTURE_REQUIRED");File raw=new File(dir,"raw.bin"),empty=new File(dir,"empty.txt");empty.createNewFile();long bytes=(96L<<20)+17;
  Random random=new Random(90821);try(FileOutputStream out=new FileOutputStream(raw)){byte[] b=new byte[32768];for(long left=bytes;left>0;){random.nextBytes(b);int k=(int)Math.min(b.length,left);out.write(b,0,k);left-=k;}out.getFD().sync();}
  ReportNativeCP4.loadForDiagnosticProcess("test:diagnostics","test");File archive=new File(dir,"archive");archive.mkdir();String id="1".repeat(64);
  try(ReportArchiveCP4 w=new ReportArchiveCP4(archive,id)){w.add("a-payload.bin",raw,4L<<20,YES);w.add("b-empty.txt",empty,4L<<20,YES);w.finish(true,true,"ACTUAL_ARCHIVE_PIPELINE_CP12");}
  final int[] partCount={0};ReportArchiveVerifierCP12.Receipt r=ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,(name,f,len,sha)->{ok(len>0&&len<=ReportArchiveCP4.PART_BYTES,"8MiB maxpart");partCount[0]++;});
  ok(r.members==2&&r.rawBytes==bytes,"complete exact payload incl zero bytes");ok(r.parts==14&&partCount[0]==14,"actual incompressible13 + empty1 parts");ok(r.evidenceComplete&&r.checksComplete,"independent footer facts");ok(r.compressedBytes>96L<<20,"no assumed compression ratio");
  reject(()->ReportArchiveVerifierCP12.verify(archive,"2".repeat(64),ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,null),"identity");
  File part=new File(archive,"P000001.bin");int value;try(RandomAccessFile io=new RandomAccessFile(part,"rw")){value=io.readUnsignedByte();io.seek(0);io.write(value^1);}reject(()->ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,null),"part altered");try(RandomAccessFile io=new RandomAccessFile(part,"rw")){io.write(value);}
  reject(()->ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),()->false,null),"cancel");
  File saved=new File(dir,"save.bin");Files.move(part.toPath(),saved.toPath());reject(()->ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,null),"part missing");Files.move(saved.toPath(),part.toPath());
  Files.write(new File(archive,"UNDECLARED.bin").toPath(),new byte[]{1});reject(()->ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,null),"undeclared part");Files.delete(new File(archive,"UNDECLARED.bin").toPath());
  File incomplete=new File(dir,"incomplete");incomplete.mkdir();try(ReportArchiveCP4 w=new ReportArchiveCP4(incomplete,id)){w.add("a",empty,4L<<20,YES);reject(()->w.add("a",empty,4L<<20,YES),"duplicate logicalmember");reject(()->w.add("catalog/a",empty,4L<<20,YES),"unsafe logicalmember");}ok(!new File(incomplete,"manifest.jsonl").exists(),"abort not published");
  ok(ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,null).rawBytes==bytes,"intact reverify");System.out.println("PASS|ArchivePipelineActualCP12|assertions="+n+"|incompressibleBytes="+bytes+"|parts="+r.parts+"|maxJavaHeap="+Runtime.getRuntime().maxMemory());
 }
}
