package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import org.json.JSONObject;

/** Explicit synthetic host fixture. No production database or model achievements are created. */
public final class ReportRecoveryContractR982 {
    private static int assertions;
    private static void check(boolean condition){assertions++;if(!condition)throw new AssertionError("recovery:"+assertions);}
    public static void main(String[] args)throws Exception {
        File root=new File(args[0]);root.mkdirs();String id=V51ChecksumEngine.sha256("synthetic-request");
        JSONObject observation=new JSONObject();String original="长诊断原文".repeat(40000);
        ReportJobJournalCP12.putObservation(root,id,observation,"crashSummary",original);
        check(observation.getJSONArray("fullObservations").length()==1);
        ReportJobJournalCP12.collectObservation(root,id,observation,"brokenModule",()->{throw new IllegalStateException("synthetic-observer-failure",new IOException("observer-original-cause"));});
        ReportJobJournalCP12.collectObservation(root,id,observation,"survivingModule",()->"independent-evidence");
        check(observation.getString("brokenModule").equals("OBSERVATION_READ_ERROR"));
        check(observation.getString("survivingModule").equals("independent-evidence"));
        String causeEvidence=observation.getString("brokenModuleError");check(causeEvidence.contains("synthetic-observer-failure")&&causeEvidence.contains("observer-original-cause"));
        boolean failedStorage=false;File blocked=new File(root,"blocked");Files.writeString(blocked.toPath(),"file-is-not-a-directory");
        try{ReportJobJournalCP12.collectObservation(blocked,id,new JSONObject(),"largeModule",()->original);}catch(IOException expected){failedStorage=true;}check(failedStorage);
        JSONObject request=new JSONObject();request.put("observation",observation);
        ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".json"),request.toString());
        IllegalStateException error=new IllegalStateException("fixture-export-unavailable",new IOException("original-root-cause"));
        JSONObject receipt=ReportJobJournalCP12.writeFailureArtifact(root,id,error);
        File archive=new File(root,"recovery/"+id+"/"+receipt.getString("sha256")+".zip");
        check(archive.length()==receipt.getLong("bytes"));check(receipt.getString("sha256").equals(ReportArchiveCP4.hash(archive,()->true)));
        try(ZipFile zip=new ZipFile(archive)) {
            check(zip.size()==4);
            String restored=new String(zip.getInputStream(zip.getEntry("observations/crashSummary.txt")).readAllBytes(),StandardCharsets.UTF_8);check(restored.equals(original));
            JSONObject boundary=new JSONObject(new String(zip.getInputStream(zip.getEntry("EVIDENCE_BOUNDARY.json")).readAllBytes(),StandardCharsets.UTF_8));
            check(!boundary.getBoolean("evidenceComplete")&&!boundary.getBoolean("checksComplete")&&!boundary.getBoolean("allChecksPassed")&&!boundary.getBoolean("databaseIncluded"));
            String cause=new String(zip.getInputStream(zip.getEntry("original-failure.txt")).readAllBytes(),StandardCharsets.UTF_8);check(cause.contains("fixture-export-unavailable")&&cause.contains("original-root-cause"));
        }
        JSONObject ack=new JSONObject();ReportJobJournalCP12.addRecoveryReceipt(root,id,ack);check(ack.getJSONObject("recovery").getString("sha256").equals(receipt.getString("sha256")));
        boolean rejected=false;try{ReportJobJournalCP12.writeFailureArtifact(root,"../outside",error);}catch(IllegalArgumentException expected){rejected=true;}check(rejected);
        File source=new File(root,"part.bin");Files.write(source.toPath(),"verified-volume".getBytes(StandardCharsets.UTF_8));String sha=ReportArchiveCP4.hash(source,()->true);File saved=new File(root,"saved.bin");
        ReportSafCopyCP12.Destination destination=new ReportSafCopyCP12.Destination(){public OutputStream openOutputTruncating()throws IOException{return new FileOutputStream(saved);}public InputStream openInput()throws IOException{return new FileInputStream(saved);}};
        check(ReportSafCopyCP12.copy(source,sha,source.length(),destination,()->true).sha256.equals(sha));
        check(ReportSafCopyCP12.verifyReadback(sha,source.length(),destination,()->true).bytes==source.length());
        Files.write(saved.toPath(),"tampered-volume".getBytes(StandardCharsets.UTF_8));rejected=false;try{ReportSafCopyCP12.verifyReadback(sha,source.length(),destination,()->true);}catch(IOException expected){rejected=true;}check(rejected);
        saved.delete();rejected=false;try{ReportSafCopyCP12.verifyReadback(sha,source.length(),destination,()->true);}catch(IOException expected){rejected=true;}check(rejected);
        rejected=false;try{ReportSafCopyCP12.copy(source,sha,source.length(),destination,()->false);}catch(IOException expected){rejected="REPORT_CANCELLED".equals(expected.getMessage())||"REPORT_SAF_CANCELLED".equals(expected.getMessage());}check(rejected);
        System.out.println("PASS|assertions="+assertions+"|scope=synthetic_host_recovery_and_readback;DEVICE_NOT_VERIFIED");
    }
}
