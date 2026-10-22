package com.ethk3.novaonepro;
import java.io.*;import java.nio.file.*;import java.lang.reflect.*;
public class CasDirectoryActualCP12 {
 static int checks;static void need(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 static void fail(Path p,long now){boolean rejected=false;try{ReportNativeCP4.trainingCasBytesCP12(p.toString(),now,true);}catch(IllegalStateException expected){rejected=expected.getMessage().startsWith("CAS_DIRECTORY_INVALID");}need(rejected,"reject directory type/name");}
 public static void main(String[] a)throws Exception{
  Path p=Path.of(a[0]);Files.createDirectories(p);long now=System.currentTimeMillis();need(ReportNativeCP4.trainingCasBytesCP12(p.toString(),now,false)==0,"empty");
  Field loaded=ReportNativeCP4.class.getDeclaredField("loaded");loaded.setAccessible(true);need(!loaded.getBoolean(null),"CAS cannot activate diagnostics");
  Files.write(p.resolve("publication.lock"),new byte[0]);Files.write(p.resolve("admission.lock"),new byte[0]);String hash="a".repeat(64);Files.write(p.resolve(hash+".scdev"),new byte[17]);Files.write(p.resolve(hash+".scbind"),new byte[23]);
  Path old=p.resolve("pending-11111111-1111-1111-1111-111111111111.partial"),young=p.resolve("pending-22222222-2222-2222-2222-222222222222.partial");Files.write(old,new byte[31]);Files.write(young,new byte[11]);need(old.toFile().setLastModified(now-86400001),"set old time");need(ReportNativeCP4.trainingCasBytesCP12(p.toString(),now,false)==82,"count all physical pending");need(ReportNativeCP4.trainingCasBytesCP12(p.toString(),now,true)==51&&!Files.exists(old)&&Files.exists(young),"only oldpartial cleanup");
  Path unknown=p.resolve("unknown");Files.write(unknown,new byte[0]);fail(p,now);Files.delete(unknown);Path link=p.resolve("b".repeat(64)+".scdev");Files.createSymbolicLink(link,p.resolve(hash+".scdev"));fail(p,now);Files.delete(link);Files.createDirectory(link);fail(p,now);Files.delete(link);Files.createLink(link,p.resolve(hash+".scdev"));fail(p,now);Files.delete(link);
  Path bad=p.resolve("pending-11111111-1111-1111-1111-11111111111g.partial");Files.write(bad,new byte[0]);fail(p,now);Files.delete(bad);need(ReportNativeCP4.trainingCasBytesCP12(p.toString(),now,true)==51,"negative tests leave exactCAS");
  boolean ioDenied=false;try{ReportNativeCP4.backup(p.toFile(),p.resolve("copy.db").toFile(),4L<<20,()->true);}catch(IOException expected){ioDenied=expected.getMessage().equals("REPORT_NATIVE_STATE");}need(ioDenied,"CAS load keeps backup denied");
  System.out.println("PASS|CasDirectoryActualCP12|assertions="+checks+"|constantNativeMemory=true|API21POSIX=true|diagnosticsIsolation=true");
 }
}
