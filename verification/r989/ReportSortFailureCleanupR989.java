package com.ethk3.novaonepro;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
/** Original and current real product runner; no substituted sort/file/Control implementation. */
public final class ReportSortFailureCleanupR989 {
 static int checks;static void need(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason+" check="+checks);}
 static String sha(File f)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath())));}
 static File input(File root,int rows)throws Exception{File file=new File(root,"original-input.jsonl");try(Writer out=new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8)){for(int i=rows-1;i>=0;i--)ReportMemberPlanCP12.append(out,String.format(Locale.ROOT,"member-%06d",i),"snapshot.db","1".repeat(64),i);}return file;}
 static void failureCase(File root,String name,int rows,ReportNativeCP4.Control control,boolean malformed,boolean conflict)throws Exception{
  File dir=new File(root,name);need(dir.mkdirs(),"fresh case");File source=input(dir,rows);if(malformed)Files.writeString(source.toPath(),"{BROKEN\n");if(conflict)try(Writer out=new FileWriter(source)){ReportMemberPlanCP12.append(out,"same","a","1".repeat(64),1);ReportMemberPlanCP12.append(out,"same","b","1".repeat(64),1);}
  String before=sha(source);File unrelated=new File(dir,"member-sort-other-job");need(unrelated.mkdir(),"unrelated directory");File marker=new File(unrelated,"retain.txt");Files.writeString(marker.toPath(),"OTHER_JOB_EVIDENCE");Exception failure=null;
  try{ReportMemberPlanCP12.sort(dir,source,control);}catch(Exception expected){failure=expected;}
  need(failure!=null,"original failure remains failure");need(sha(source).equals(before),"original input hash preserved");need(Files.readString(marker.toPath()).equals("OTHER_JOB_EVIDENCE"),"other job preserved");
  need(malformed?failure instanceof IllegalArgumentException&&"REPORT_SORT_RECORD".equals(failure.getMessage()):conflict?"REPORT_MEMBER_ALIAS_CONFLICT".equals(failure.getMessage()):rows==0?"REPORT_NO_MEMBERS".equals(failure.getMessage()):failure instanceof InterruptedIOException&&"REPORT_CANCELLED".equals(failure.getMessage()),"exact original failure type and reason retained");
  File[] owned=dir.listFiles(f->f.getName().startsWith("members-")||(f.getName().startsWith("member-sort-")&&!f.equals(unrelated)));need(owned!=null&&owned.length==0,"failed attempt generated files reclaimed: "+name);
 }
 public static void main(String[] args)throws Exception{
  File root=new File(args[0]);need(root.mkdirs(),"fresh root");
  failureCase(root,"cancel-before-read",97,()->false,false,false);
  AtomicInteger spool=new AtomicInteger();failureCase(root,"cancel-after-spool",97,()->spool.incrementAndGet()<50,false,false);
  AtomicInteger merge=new AtomicInteger();failureCase(root,"cancel-during-merge",1000,()->merge.incrementAndGet()<1100,false,false);
  failureCase(root,"duplicate-identity-conflict",0,()->true,false,true);
  failureCase(root,"malformed-member",0,()->true,true,false);
  failureCase(root,"empty-members",0,()->true,false,false);
  Thread.currentThread().interrupt();try{failureCase(root,"thread-interrupted",97,()->true,false,false);need(Thread.currentThread().isInterrupted(),"interrupt retained");}finally{Thread.interrupted();}
  File guarded=new File(root,"cleanup-path-failure");need(guarded.mkdir(),"fresh guarded root");File guardedInput=input(guarded,97),outside=new File(root,"outside.txt");Files.writeString(outside.toPath(),"OUTSIDE_PROTECTED");String outsideHash=sha(outside);Exception cancellationFailure=null;
  try{ReportMemberPlanCP12.sort(guarded,guardedInput,()->{try{File work=guarded.listFiles(f->f.getName().matches("member-sort-[0-9a-f]{32}"))[0];Files.createSymbolicLink(new File(work,"escape").toPath(),outside.toPath());return false;}catch(IOException failure){throw new UncheckedIOException(failure);}});}catch(Exception expected){cancellationFailure=expected;}
  need(cancellationFailure instanceof InterruptedIOException&&"REPORT_CANCELLED".equals(cancellationFailure.getMessage()),"cleanup failure preserves exact original cancellation");need(Arrays.stream(cancellationFailure.getSuppressed()).anyMatch(x->"REPORT_SORT_CLEANUP_PATH".equals(x.getMessage())),"cleanup failure explicitly attached");need(sha(outside).equals(outsideHash),"cleanup never follows symlink outside attempt");need(guarded.listFiles(f->f.getName().startsWith("members-")).length==0,"failed cleanup never publishes result");
  File success=new File(root,"success");need(success.mkdir(),"fresh success");File original=input(success,97);String before=sha(original);File sorted=ReportMemberPlanCP12.sort(success,original,()->true);need(sorted.isFile(),"successful result retained");need(sha(original).equals(before),"successful original retained");List<String> lines=Files.readAllLines(sorted.toPath());need(lines.size()==97,"all sorted rows");for(int i=0;i<97;i++){org.json.JSONObject row=new org.json.JSONObject(lines.get(i));need(row.getString("name").equals(String.format(Locale.ROOT,"member-%06d",i))&&row.getLong("bytes")==i,"actual ordered values");}need(success.listFiles(f->f.getName().startsWith("member-sort-")).length==0,"successful workspace removed");
  System.out.println("PASS ACTUAL_REPORT_SORT_FAILURE_CLEANUP checks="+checks+" original_inputs_and_other_jobs_preserved=true scope=HOST_FILESYSTEM_DEVICE_NOT_VERIFIED");
 }
}
